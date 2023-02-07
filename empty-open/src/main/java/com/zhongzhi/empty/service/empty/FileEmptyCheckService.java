package com.zhongzhi.empty.service.empty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.constants.EmptyRedisKeyConstant;
import com.zhongzhi.empty.constants.RedisKeyConstant;
import com.zhongzhi.empty.entity.CustomerConsume;
import com.zhongzhi.empty.entity.CvsFilePath;
import com.zhongzhi.empty.entity.EmptyCheck;
import com.zhongzhi.empty.entity.EmptyNumFileDetectionResult;
import com.zhongzhi.empty.entity.FileDetectionEnumAndDelivrdSet;
import com.zhongzhi.empty.entity.RunTestDomian;
import com.zhongzhi.empty.entity.SysConfig;
import com.zhongzhi.empty.entity.TestResultData;
import com.zhongzhi.empty.entity.TxtFileContent;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.enums.MobileReportGroupEnum;
import com.zhongzhi.empty.enums.ProductEnum;
import com.zhongzhi.empty.enums.TxtSuffixEnum;
import com.zhongzhi.empty.redis.DistributedLockWrapper;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.LuaByDeductFeeResponse;
import com.zhongzhi.empty.response.UnnMobileStatus;
import com.zhongzhi.empty.service.SysConfigService;
import com.zhongzhi.empty.service.ThreadExecutorService;
import com.zhongzhi.empty.service.balance.BalanceService;
import com.zhongzhi.empty.service.customer.CustomerConsumeService;
import com.zhongzhi.empty.service.file.FileRedisService;
import com.zhongzhi.empty.service.file.FileService;
import com.zhongzhi.empty.service.file.FileUploadService;
import com.zhongzhi.empty.util.DingDingMessage;
import com.zhongzhi.empty.vo.UnnResultVo;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 文件空号检测
 * @author liuh
 * @date 2021年10月29日
 */
@Slf4j
@Service
public class FileEmptyCheckService {
	
	@Autowired
	private JedisPool jedisPool;
	
	@Autowired
	private FileRedisService fileRedisService;
	
	@Autowired
	private FileService fileService;
	
	@Autowired
	private RedisClient redisClient;
	
	@Autowired
	private BalanceService balanceService;
	
	@Autowired
	private DingDingMessage dingDingMessage;
	
	@Autowired
    private ThreadExecutorService threadExecutorService;
	
	@Autowired
	private UnnApiService unnApiService;
	
	@Autowired
	private SysConfigService sysConfigService;
	
	@Autowired
	private EmptyCheckService emptyCheckService;
	
	@Autowired
	private CvsFilePathService cvsFilePathService;
	
	@Autowired
	private CustomerConsumeService customerConsumeService;
	
	@Autowired
	private FileUploadService fileUploadService;
	
	private static final int BATCH_NUM_SIZE = 2000;

	public ApiResult executeEmptyCheck(Long customerId,Long emptyId,Long totalNumber, String sourceFileName,String uploadPath) {
		int expire = 60 * 60 * 1000; //超时时间
        //1.设置redis锁
        DistributedLockWrapper lock = new DistributedLockWrapper(jedisPool, String.format(EmptyRedisKeyConstant.THE_TEST_FUN_KEY, 
        		customerId,emptyId), 1000L * 60 * 60, expire);
        if (StringUtils.isBlank(lock.getIdentifier())) {
        	log.error("{}, 该文件已提交检测，emptyId:{}",customerId,emptyId);
        	return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "已提交检测，请勿重复提交", null);
        }
        
    	log.info("----------用户[{}]开始执行空号检索事件,emptyId：{}",customerId,emptyId);
    	//2.初始化在线检测相关配置redis值
    	fileRedisService.theTestRedisInit(customerId, emptyId, expire, lock.getIdentifier(),totalNumber);             
        //3.获取有效的手机号码以及文件编码格式
    	TxtFileContent fileContent = fileService.getValidMobileListByTxt(uploadPath);
        if (fileContent.getMobileCounts() <= 0) {
        	lock.releaseLock();
            redisClient.set(String.format(EmptyRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,emptyId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            return ApiResult.result(ApiCode.COUNT_EXCEPTION, "待检号码统计为0或统计失败", null);
        }
        
        int mobileCount = fileContent.getMobileCounts();
        //4.冻结账户余额
        Boolean isFreeze = balanceService.preDeductFee(customerId, mobileCount, ProductEnum.EMPTY.getProductCode(),emptyId);
        if (!isFreeze) {
        	lock.releaseLock();
            redisClient.set(String.format(EmptyRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,emptyId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足或预扣失败", null);
        }
        
        //5.提交的有效号码总个数存入redis
        String addFlag = redisClient.set(String.format(EmptyRedisKeyConstant.SUCCEED_CLEARING_COUNT_KEY, 
        								customerId,emptyId),String.valueOf(mobileCount),expire);
        if(StringUtils.isBlank(addFlag) || !CommonConstant.REDIS_SET_RETURN.equals(addFlag)) {
        	lock.releaseLock();
            redisClient.set(String.format(EmptyRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,emptyId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            //返还预扣费
            balanceService.backDeductFee(customerId, mobileCount,ProductEnum.EMPTY.getProductCode(),emptyId);
            return ApiResult.result(ApiCode.COUNT_EXCEPTION, "计数异常", null);
        }
        
        try {
	        //6.保存临时文件
	        fileService.saveTempFileByAll(uploadPath, customerId, fileContent.getMobileList(), emptyId);
	        //7.执行检测
	        emptyNumFileDetectionByTxtNew(mobileCount, customerId, expire, lock, uploadPath,
	        		                        emptyId, fileContent.getFileCode(),sourceFileName,fileContent.getErrorCounts());
        } catch (IOException e) {
			lock.releaseLock();
			//返还预扣费
            balanceService.backDeductFee(customerId, mobileCount,ProductEnum.EMPTY.getProductCode(),emptyId);
            
			log.error("{}, 用户空号在线检测异常，emptyId:{},info:", customerId,emptyId,e);
			dingDingMessage.sendMessage(String.format("警告： 用户【%s】空号在线检测生成临时文件异常，emptyId:%s，info:%s", customerId,emptyId,e));
			return ApiResult.fail("空号检测异常");
		}
        
        lock.releaseLock();
		return ApiResult.ok(new RunTestDomian(getPauseSecond(mobileCount),CommonConstant.THETEST_RUNNING,emptyId.toString()));
	}
	
	private int getPauseSecond(int size){
    	int result = 2;
    	if(size >= 6000 && size < 12000){
    		result = 3;
    	}else if(size >= 12000 && size < 50000){
    		result = 4;
    	}else if(size >= 50000 && size < 500000){
    		result = 5;
    	}else if(size >= 3000 && size < 6000){
    		result = 2;
    	}else{
    		result = 6;
    	}
		return result;    	
    }
	
	/**
     * 空号文件批量检查，大数据通道
     */
    private Future<?> emptyNumFileDetectionByTxtNew(int mobileCount, Long customerId,
                                                              Integer expire,DistributedLockWrapper lock,
                                                              String fileUrl,Long emptyId, String fileEncoding,String sourceFileName,Integer errorCounts) {
    	Runnable run = new Runnable() {
			public void run() {
				Jedis jedis = null;
                try {
                    jedis = jedisPool.getResource();
                    //出现异常终止检测
                    String exceptions = jedis.get(String.format(EmptyRedisKeyConstant.EXCEPTION_KEY, customerId,emptyId));
                    if (CommonConstant.FILE_TESTING_CODE.equals(exceptions)) {                      	
                        long beginTime = System.currentTimeMillis();
                        //新建检查进度记录
                        log.info(">>>>>>>>>>>>>>> 空号文件开始检查 emptyId:{}，数量:{}>>>>>>>>>>>>>>>",emptyId,mobileCount);                       
                        //批次数    分批检测手机号状态，每次2000个
                        int batchCount = mobileCount / BATCH_NUM_SIZE;
                        for (int i = 0; i < (batchCount + 1); i++) {                      
                            //检测的号码标志位
                            int fromIndex = BATCH_NUM_SIZE * i + 1;
                            int toIndex = (fromIndex + BATCH_NUM_SIZE - 1)>mobileCount?mobileCount:(fromIndex + BATCH_NUM_SIZE - 1);
                            //调用空号检测接口
                            emptyNumFileDetection(fileService.readTxtFileContent(fileUrl, fileEncoding, fromIndex),customerId,fileUrl,emptyId);                            
                            //等待1.2秒
                            Thread.sleep(10);
                            log.info(">>>>>>>>>>>>>>> 空号文件检测中， emptyId:{}，当前进度:{}/{}>>>>>>>>>>>>>>>",emptyId,toIndex,mobileCount);
                            // 成功检测条数累加
                            redisClient.set(String.format(EmptyRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, customerId,emptyId), String.valueOf(toIndex), expire);
                        }
                        //完成后等待30秒
                        Thread.sleep((new Random().nextInt(20000)) + 5000);
                        log.info(">>>>>>>>>>>>>>> 空号文件结束检查，等待生成txt，emptyId:{}，数量:{}，用时:{} >>>>>>>>>>>>>>>",emptyId,mobileCount,(System.currentTimeMillis() - beginTime));
                        //生成结果文件
                        generateResultFiles(fileUrl, customerId, emptyId, sourceFileName, mobileCount, expire,errorCounts);
                        log.info(">>>>>>>>>>>>>>> 空号文件完成任务，emptyId:{}，数量:{}，用时:{} >>>>>>>>>>>>>>>",emptyId,mobileCount,(System.currentTimeMillis() - beginTime));
                    }
                } catch (Exception e) {
                    log.error("{}, 线程执行空号检测异常, emptyId:{},info:",customerId,emptyId, e);
                    dingDingMessage.sendMessage(String.format("警告：%s,执行空号检测出现系统异常,emptyId:%s,info:%s", customerId,emptyId,e));
                    //返还预扣费
                    balanceService.backDeductFee(customerId, mobileCount, ProductEnum.EMPTY.getProductCode(),emptyId);
                } finally {
                    // 返还到连接池
                    jedis.close();
                }
			}
		};
		
		// 加入线程池开始执行
        return threadExecutorService.execute(run);
    }
    
    /**
     * 生成结果报表
     */
    @Transactional
    private void generateResultFiles(String fileUrl, Long customerId, Long emptyId, String sourceFileName,int mobileCount,Integer expire,Integer errorCounts) {
        try {
            //获取检测结果
            TestResultData testResultData = fileService.getTestResultByTxtFile(fileUrl, customerId, emptyId, sourceFileName);          
            if (testResultData.getTotalCount() == 0 ) {
            	//返还预扣费
                balanceService.backDeductFee(customerId, mobileCount, ProductEnum.EMPTY.getProductCode(),emptyId);
                redisClient.set(String.format(EmptyRedisKeyConstant.EXCEPTION_KEY, customerId, emptyId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
                return;
            }
                    	
            CvsFilePath cvsFilePath = testResultData.getCvsFilePath();
            int counts = cvsFilePathService.saveOne(cvsFilePath);
            if(counts != 1) {
            	log.error("{}, 空号文件检测成功，但文件结果包信息记录插入失败，info:{}",customerId,JSON.toJSONString(cvsFilePath));
            	dingDingMessage.sendMessage(String.format("警告：%s, 空号在线检测成功，但文件结果包信息记录插入失败，emptyId:%s", customerId,emptyId));
            	return ;
            }
            
            //执行扣费操作
            LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerId, mobileCount, 
            		                    cvsFilePath.getTotalNumber(), ProductEnum.EMPTY.getProductCode(),emptyId);
            
            // 将空号检测查询接口返回的检测数据保存到数据库，同时更新空号检测订单状态
            String unknownNumberStr = redisClient.get(String.format(RedisKeyConstant.FILE_REAL_API_CACHE_KEY, customerId,emptyId));
            EmptyCheck emptyCheck = new EmptyCheck();
            emptyCheck.setId(emptyId).setStatus(EmptyCheck.EmptyCheckStatus.WORK_FINISH.getStatus())
			            .setRealNumber(Long.valueOf(cvsFilePath.getRealNumber()==null?0:cvsFilePath.getRealNumber()))
			            .setEmptyNumber(Long.valueOf(cvsFilePath.getEmptyNumber()==null?0:cvsFilePath.getEmptyNumber()))
			            .setRiskNumber(Long.valueOf(cvsFilePath.getRiskNumber()==null?0:cvsFilePath.getRiskNumber()))
			            .setSilentNumber(Long.valueOf(cvsFilePath.getSilentNumber()==null?0:cvsFilePath.getSilentNumber()))
			            .setTotalNumber(Long.valueOf(cvsFilePath.getTotalNumber()==null?0:cvsFilePath.getTotalNumber()))
			            .setIllegalNumber(Long.valueOf(errorCounts))
			            .setUnknownNumber(StringUtils.isBlank(unknownNumberStr)?0:Long.valueOf(unknownNumberStr))
			            .setLine(String.valueOf(cvsFilePath.getTotalNumber()==null?0:cvsFilePath.getTotalNumber()));
            counts = emptyCheckService.updateOne(emptyCheck);
            if(counts < 1) {
            	log.error("{}, 空号文件检测成功，但检测记录更新失败，info:{}",customerId,JSON.toJSONString(emptyCheck));
            	dingDingMessage.sendMessage(String.format("警告：%s, 空号在线检测成功，但检测记录更新失败，emptyId:%s", customerId,emptyId));
            	return ;
            }
            
            //设置程序运行结束
            redisClient.set(String.format(EmptyRedisKeyConstant.THE_RUN_KEY, customerId, emptyId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            //文件md5存入缓存
            fileUploadService.handleFileMd5Cache(emptyId,customerId);
            //删除文件实际调用外部通道条数缓存
            redisClient.remove(String.format(RedisKeyConstant.FILE_REAL_API_CACHE_KEY, customerId,emptyId));
            
            CustomerConsume consume = new CustomerConsume();
            consume.setEmptyId(emptyId)
                   .setCustomerId(customerId)
                   .setConsumeNumber(Long.valueOf(cvsFilePath.getTotalNumber()))
                   .setConsumeType(CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue())
                   .setClosingBalance(luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance())
                   .setOpeningBalance(consume.getClosingBalance() + consume.getConsumeNumber());
             counts = customerConsumeService.updateOne(consume);
             if(counts < 1) {
             	log.error("{}, 空号文件检测成功，但消耗记录更新失败，info:{}",customerId,JSON.toJSONString(consume));
             	dingDingMessage.sendMessage(String.format("警告：%s, 空号在线检测成功，但消耗记录更新失败，emptyId:%s", customerId,emptyId));
             	return ;
             }
            
            log.info("----------用户编号：[{}]结束执行空号检索事件 ",customerId);            
        } catch (Exception e) {            
        	//返还预扣费
            balanceService.backDeductFee(customerId, mobileCount, ProductEnum.EMPTY.getProductCode(),emptyId);
            redisClient.set(String.format(EmptyRedisKeyConstant.EXCEPTION_KEY, customerId, emptyId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            log.error("{},线程执行空号检测异常,emptyId:{},info:",customerId,emptyId,e);
            dingDingMessage.sendMessage(String.format("警告：%s,线程执行空号检测生成文件异常,emptyId:%s,info:%s",customerId,emptyId,e));
        }
    }
    
    private void emptyNumFileDetection(List<String> mobileSubList,Long customerId,String fileUrl,Long emptyId) throws Exception{
    	if(CollectionUtils.isEmpty(mobileSubList)) {
    		return ;
    	}
    	
   	   //查询检测接口
       EmptyNumFileDetectionResult detectionResult = emptyNumFileDetectionNew(mobileSubList, customerId,emptyId);        
       if (detectionResult != null && detectionResult.getData() != null) {
           ListMultimap<MobileReportGroupEnum, String> group = detectionResult.getData();
           //数据分组存入文本
           fileService.saveTestResultData(customerId, fileUrl, emptyId, group);
           //没有返回结果，直接存入沉默包
           fileService.saveMobileToTxtFile(fileUrl, new ArrayList<>(group.get(MobileReportGroupEnum.FileDetection.NO_RESULT)), TxtSuffixEnum.SILENCE);
       }else{
       	//大数据返回为空重试
       	log.error(">>>>>>>>>>>>>>> 空号文件检测异常，检测接口第一次调用返回为空， emptyId:{}>>>>>>>>>>>>>>>",emptyId);                                	
       	//根据检测状态码列表判断手机号状态
        EmptyNumFileDetectionResult reDetectionResult = emptyNumFileDetectionNew(mobileSubList, customerId,emptyId);  
       	if(reDetectionResult == null || reDetectionResult.getData() == null){
       		//第二次重试失败则这批号码直接放到沉默号
       		log.error(">>>>>>>>>>>>>>> 空号文件检测异常，检测接口第二次重试调用返回为空， emptyId:{}>>>>>>>>>>>>>>>",emptyId); 
       		fileService.saveMobileToTxtFile(fileUrl, mobileSubList, TxtSuffixEnum.SILENCE);
       	}else{
           ListMultimap<MobileReportGroupEnum, String> group = reDetectionResult.getData();
           //数据分组存入文本
           fileService.saveTestResultData(customerId, fileUrl, emptyId, group);
           //没有返回结果，直接存入沉默包
           fileService.saveMobileToTxtFile(fileUrl, new ArrayList<>(group.get(MobileReportGroupEnum.FileDetection.NO_RESULT)), TxtSuffixEnum.SILENCE);
       	}                              	                                	
       }       
   }
    
    public EmptyNumFileDetectionResult emptyNumFileDetectionNew(List<String> mobileList,Long customerId,Long emptyId) throws Exception {
        EmptyNumFileDetectionResult result = new EmptyNumFileDetectionResult();
        ListMultimap<MobileReportGroupEnum, String> data = ArrayListMultimap.create();
        // 获取系统当前设置的空号检测通道
 		String gateway = "";
 		SysConfig sysConfig = sysConfigService.findOneByKey(CommonConstant.EMPTY_GATEWAY);
 		gateway = (sysConfig == null || sysConfig.getStatus() == 0) ? CommonConstant.EMPTY_GATEWAY_ONLINE_DEFAULT : sysConfig.getParamValue();
        
        Set<String> mobileSet = new HashSet<>(mobileList);
 
        List<UnnMobileStatus> unnMobileStatusList = new ArrayList<UnnMobileStatus>();
        //请求api批量检查号码
        UnnResultVo unnResultVo = unnApiService.invokeApi(customerId, gateway, mobileSet.toArray(new String[mobileSet.size()]));
        if(unnResultVo != null) {
        	unnMobileStatusList = unnResultVo.getList();
        }
        Set<String> hasResultSet = new HashSet<>();
        if(!CollectionUtils.isEmpty(unnMobileStatusList)){
        	for (UnnMobileStatus unnMobileStatus : unnMobileStatusList) {
                String m = unnMobileStatus.getMobile();
                hasResultSet.add(m);
                //1.遍历实号状态
                FileDetectionEnumAndDelivrdSet fileDetectionEnumAndDelivrdSet = emptyNumFileDetectionDelivrdNew(m,unnMobileStatus.getStatus());
                if (fileDetectionEnumAndDelivrdSet != null) {
                    if (fileDetectionEnumAndDelivrdSet.getDetectedStatus() == 0) {
                        //设置分组
                        MobileReportGroupEnum.FileDetection eFileDetectionEnum = fileDetectionEnumAndDelivrdSet.getFd();
                        //分组
                        if (eFileDetectionEnum != null) {
                            data.put(eFileDetectionEnum, m);
                            continue;
                        }
                    }
                }
                
                //2.如果都不包括，则分组为：没有返回结果
                data.put(MobileReportGroupEnum.FileDetection.NO_RESULT, m);
            }
        }
        
        if (!CollectionUtils.isEmpty(hasResultSet)) {
        	mobileList.removeAll(hasResultSet);
		}
        
        if (!CollectionUtils.isEmpty(mobileList)) {
			for(String mm : mobileList) {
				data.put(MobileReportGroupEnum.FileDetection.NO_RESULT, mm);
			}
		}
        
        //实际调用外部接口的条数存入redis
        if(unnResultVo != null && unnResultVo.getNoCacheCount() > 0) {
        	redisClient.incrBy(String.format(RedisKeyConstant.FILE_REAL_API_CACHE_KEY, customerId,emptyId), unnResultVo.getNoCacheCount());
        }
        
        result.setData(data);
        return result;
    }
    
	private FileDetectionEnumAndDelivrdSet emptyNumFileDetectionDelivrdNew(String mobile,String status) {
		FileDetectionEnumAndDelivrdSet result = new FileDetectionEnumAndDelivrdSet();
		//检测结果  0：有正常结果   1：状态未知   2：没有结果
		result.setDetectedStatus(2);
		if (StringUtils.isNotBlank(status)) {
			//文件检测分组枚举
			 MobileReportGroupEnum.FileDetection fileDetectionEnum = MobileReportGroupEnum.FileDetection.fromBackGroupCode(status);
			if (fileDetectionEnum != null) {
				result.setFd(fileDetectionEnum);
				//检测结果  0：有正常结果   1：状态未知   2：没有结果
				result.setDetectedStatus(0);
				return result;
			}	
		}
		
		result.setFd(MobileReportGroupEnum.FileDetection.NO_RESULT);
		return result;
	}
}

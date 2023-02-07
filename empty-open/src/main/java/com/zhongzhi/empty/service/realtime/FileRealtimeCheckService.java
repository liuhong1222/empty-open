package com.zhongzhi.empty.service.realtime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.constants.RealtimeRedisKeyConstant;
import com.zhongzhi.empty.entity.CustomerConsume;
import com.zhongzhi.empty.entity.EmptyCheck;
import com.zhongzhi.empty.entity.RealtimeCheck;
import com.zhongzhi.empty.entity.RealtimeCvsFilePath;
import com.zhongzhi.empty.entity.RealtimeFileDetectionEnumAndDelivrdSet;
import com.zhongzhi.empty.entity.RealtimeFileDetectionResult;
import com.zhongzhi.empty.entity.RealtimeResultData;
import com.zhongzhi.empty.entity.RunTestDomian;
import com.zhongzhi.empty.entity.TxtFileContent;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.enums.ProductEnum;
import com.zhongzhi.empty.enums.RealtimeReportGroupEnum;
import com.zhongzhi.empty.enums.RealtimeTxtSuffixEnum;
import com.zhongzhi.empty.http.realtime.MobileRealtimeStatus;
import com.zhongzhi.empty.redis.DistributedLockWrapper;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.LuaByDeductFeeResponse;
import com.zhongzhi.empty.service.ThreadExecutorService;
import com.zhongzhi.empty.service.balance.BalanceService;
import com.zhongzhi.empty.service.customer.CustomerConsumeService;
import com.zhongzhi.empty.service.file.FileRedisService;
import com.zhongzhi.empty.service.file.FileUploadService;
import com.zhongzhi.empty.service.gateway.RealtimeService;
import com.zhongzhi.empty.util.DingDingMessage;
import com.zhongzhi.empty.util.ListUtils;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 文件实时检测
 * @author liuh
 * @date 2021年11月3日
 */
@Slf4j
@Service
public class FileRealtimeCheckService {
	
	@Autowired
	private JedisPool jedisPool;
	
	@Autowired
	private FileRedisService fileRedisService;
	
	@Autowired
	private RealtimeFileService realtimeFileService;
	
	@Autowired
	private RedisClient redisClient;
	
	@Autowired
	private BalanceService balanceService;
	
	@Autowired
	private DingDingMessage dingDingMessage;
	
	@Autowired
    private ThreadExecutorService threadExecutorService;
	
	@Autowired
	private RealtimeService realtimeService;
	
	@Autowired
	private RealtimeCvsFilePathService realtimeCvsFilePathService;
	
	@Autowired
	private RealtimeCheckService realtimeCheckService;
	
	@Autowired
	private CustomerConsumeService customerConsumeService;
	
	@Autowired
	private FileUploadService fileUploadService;
	
	@Value("${realtime.thread.num}")
	private Integer NUM_INTEGER;
	
	private ExecutorService executor = Executors.newFixedThreadPool(100);
	
	private static final int BATCH_NUM_SIZE = 2000;

	public ApiResult executeRealtimeCheck(Long customerId,Long realtimeId,Long totalNumber, String sourceFileName,String uploadPath) {
		int expire = 3 * 60 * 60 * 1000; //超时时间
        //1.设置redis锁
        DistributedLockWrapper lock = new DistributedLockWrapper(jedisPool, String.format(RealtimeRedisKeyConstant.THE_TEST_FUN_KEY, 
        		customerId,realtimeId), 1000L * 60 * 60, expire);
        if (StringUtils.isBlank(lock.getIdentifier())) {
        	log.error("{}, 该文件已提交检测，emptyId:{}",customerId,realtimeId);
        	return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "已提交检测，请勿重复提交", null);
        }
        
    	log.info("----------用户[{}]开始执行实时检测检索事件,emptyId：{}",customerId,realtimeId);
    	//2.初始化实时检测相关配置redis值
    	fileRedisService.realtimeRedisInit(customerId, realtimeId, expire, lock.getIdentifier(),totalNumber);             
        //3.获取有效的手机号码以及文件编码格式
    	TxtFileContent fileContent = realtimeFileService.getValidMobileListByTxt(uploadPath);
        List<String> mobileList = fileContent.getMobileList();                
        if (CollectionUtils.isEmpty(mobileList)) {
        	lock.releaseLock();
            redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,customerId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            return ApiResult.result(ApiCode.COUNT_EXCEPTION, "待检号码统计为0或统计失败", null);
        }
        
        int mobileCount = mobileList.size();
        //4.冻结账户余额
        Boolean isFreeze = balanceService.preDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),realtimeId);
        if (!isFreeze) {
        	lock.releaseLock();
            redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足或预扣失败", null);
        }
        
        //5.提交的有效号码总个数存入redis
        String addFlag = redisClient.set(String.format(RealtimeRedisKeyConstant.SUCCEED_CLEARING_COUNT_KEY, 
        								customerId,realtimeId),String.valueOf(mobileCount),expire);
        if(StringUtils.isBlank(addFlag) || !CommonConstant.REDIS_SET_RETURN.equals(addFlag)) {
        	lock.releaseLock();
            redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            //返还预扣费
            balanceService.backDeductFee(customerId, mobileCount,ProductEnum.REALTIME.getProductCode(),realtimeId);
            return ApiResult.result(ApiCode.COUNT_EXCEPTION, "计数异常", null);
        }
        
        try {
	        //6.保存临时文件
        	realtimeFileService.saveTempFileByAll(uploadPath, customerId, mobileList, realtimeId);
	        //7.执行检测
	        realtimeFileDetectionByTxtNew(mobileCount, customerId, expire, lock, uploadPath,
	        									realtimeId, fileContent.getFileCode(),sourceFileName,fileContent.getErrorCounts());
        } catch (IOException e) {
			lock.releaseLock();
			//返还预扣费
            balanceService.backDeductFee(customerId, mobileCount,ProductEnum.REALTIME.getProductCode(),realtimeId);
            
			log.error("{}, 用户实时在线检测异常，emptyId:{},info:", customerId,realtimeId,e);
			dingDingMessage.sendMessage(String.format("警告： 用户【%s】实时在线检测生成临时文件异常，emptyId:%s，info:%s", customerId,realtimeId,e));
			return ApiResult.fail("实时检测异常");
		}
        
        lock.releaseLock();
		return ApiResult.ok(new RunTestDomian(getPauseSecond(mobileList.size()),CommonConstant.THETEST_RUNNING,realtimeId.toString()));
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
    private Future<?> realtimeFileDetectionByTxtNew(int mobileCount, Long customerId,
                                                              Integer expire,DistributedLockWrapper lock,
                                                              String fileUrl,Long realtimeId, String fileEncoding,String sourceFileName,Integer errorCounts) {
    	Runnable run = new Runnable() {
			public void run() {
				Jedis jedis = null;
                try {
                    jedis = jedisPool.getResource();
                    //出现异常终止检测
                    String exceptions = jedis.get(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, customerId,realtimeId));
                    if (CommonConstant.FILE_TESTING_CODE.equals(exceptions)) {                      	
                        long beginTime = System.currentTimeMillis();
                        //新建检查进度记录
                        log.info(">>>>>>>>>>>>>>> 实时文件开始检查 realtimeId:{}，数量:{}>>>>>>>>>>>>>>>",realtimeId,mobileCount);                       
                        //批次数    分批检测手机号状态，每次2000个
                        int batchCount = mobileCount / BATCH_NUM_SIZE;
                        for (int i = 0; i < (batchCount + 1); i++) {                            
                            //检测的号码标志位
                            int fromIndex = BATCH_NUM_SIZE * i + 1;
                            int toIndex = (fromIndex + BATCH_NUM_SIZE - 1)>mobileCount?mobileCount:(fromIndex + BATCH_NUM_SIZE - 1);
                            //调用实时检测接口
                            realtimeFileDetection(realtimeFileService.readTxtFileContent(fileUrl, fileEncoding, fromIndex),customerId,fileUrl,realtimeId);                            
                            //等待1.2秒
                            Thread.sleep(200);
                            log.info(">>>>>>>>>>>>>>> 实时文件检测中， realtimeId:{}，当前进度:{}/{}>>>>>>>>>>>>>>>",realtimeId,toIndex,mobileCount);
                            // 成功检测条数累加
                            redisClient.set(String.format(RealtimeRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, customerId,realtimeId), String.valueOf(toIndex), expire);
                        }
                        //完成后等待30秒
                        Thread.sleep((new Random().nextInt(20000)) + 5000);
                        log.info(">>>>>>>>>>>>>>> 实时文件结束检查，等待生成txt，realtimeId:{}，数量:{}，用时:{} >>>>>>>>>>>>>>>",realtimeId,mobileCount,(System.currentTimeMillis() - beginTime));
                        //生成结果文件
                        generateResultFiles(fileUrl, customerId, realtimeId, sourceFileName, mobileCount, expire,errorCounts);
                        log.info(">>>>>>>>>>>>>>> 实时文件完成任务，realtimeId:{}，数量:{}，用时:{} >>>>>>>>>>>>>>>",realtimeId,mobileCount,(System.currentTimeMillis() - beginTime));
                    }
                } catch (Exception e) {
                    log.error("{}, 线程执行实时检测异常, realtimeId:{},info:",customerId,realtimeId, e);
                    dingDingMessage.sendMessage(String.format("警告：%s,执行实时检测出现系统异常,realtimeId:%s,info:%s", customerId,realtimeId,e));
                    //返还预扣费
                    balanceService.backDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),realtimeId);
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
    private void generateResultFiles(String fileUrl, Long customerId, Long realtimeId, String sourceFileName,int mobileCount,Integer expire,Integer errorCounts) {
        try {
            //获取检测结果
        	RealtimeResultData realtimeResultData = realtimeFileService.getTestResultByTxtFile(fileUrl, customerId, realtimeId, sourceFileName);          
            if (realtimeResultData.getTotalCount() == 0 ) {
            	//返还预扣费
                balanceService.backDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),realtimeId);
                redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, customerId, realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
                return;
            }
                    	
            RealtimeCvsFilePath realtimeCvsFilePath = realtimeResultData.getRealtimeCvsFilePath();
            int counts = realtimeCvsFilePathService.saveOne(realtimeCvsFilePath);
            if(counts != 1) {
            	log.error("{}, 实时文件检测成功，但文件结果包信息记录插入失败，info:{}",customerId,JSON.toJSONString(realtimeCvsFilePath));
            	dingDingMessage.sendMessage(String.format("警告：%s, 实时在线检测成功，但文件结果包信息记录插入失败，emptyId:%s", customerId,customerId));
            	return ;
            }
            
            //执行扣费操作
            LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerId, mobileCount, 
            		realtimeCvsFilePath.getTotalNumber()-(realtimeCvsFilePath.getUnknownNumber()==null?0:realtimeCvsFilePath.getUnknownNumber()), ProductEnum.REALTIME.getProductCode(),realtimeId);
            
            // 将空号检测查询接口返回的检测数据保存到数据库，同时更新空号检测订单状态
            RealtimeCheck realtimeCheck = new RealtimeCheck();
            realtimeCheck.setId(realtimeId).setStatus(EmptyCheck.EmptyCheckStatus.WORK_FINISH.getStatus())
            		.setNormal(Long.valueOf(realtimeCvsFilePath.getNormalNumber()==null?0:realtimeCvsFilePath.getNormalNumber()))
            		.setEmpty(Long.valueOf(realtimeCvsFilePath.getEmptyNumber()==null?0:realtimeCvsFilePath.getEmptyNumber()))
            		.setOnCall(Long.valueOf(realtimeCvsFilePath.getOncallNumber()==null?0:realtimeCvsFilePath.getOncallNumber()))
            		.setOnlineButNotAvailable(Long.valueOf(realtimeCvsFilePath.getNotOnlineNumber()==null?0:realtimeCvsFilePath.getNotOnlineNumber()))
            		.setShutdown(Long.valueOf(realtimeCvsFilePath.getShutdownNumber()==null?0:realtimeCvsFilePath.getShutdownNumber()))
            		.setSuspectedShutdown(Long.valueOf(realtimeCvsFilePath.getLikeShutdownNumber()==null?0:realtimeCvsFilePath.getLikeShutdownNumber()))
            		.setServiceSuspended(Long.valueOf(realtimeCvsFilePath.getTingjiNumber()==null?0:realtimeCvsFilePath.getTingjiNumber()))
            		.setNumberPortability(Long.valueOf(realtimeCvsFilePath.getMnpNumber()==null?0:realtimeCvsFilePath.getMnpNumber()))
            		.setUnknown(Long.valueOf(realtimeCvsFilePath.getMoberrNumber()==null?0:realtimeCvsFilePath.getMoberrNumber()))
            		.setExceptionFailCount(Long.valueOf(realtimeCvsFilePath.getUnknownNumber()==null?0:realtimeCvsFilePath.getUnknownNumber()))
            		.setIllegalNumber(Long.valueOf(errorCounts))
            		.setTotalNumber(Long.valueOf(realtimeCvsFilePath.getTotalNumber()==null?0:realtimeCvsFilePath.getTotalNumber()))
                    .setLine(String.valueOf(realtimeCvsFilePath.getTotalNumber()==null?0:realtimeCvsFilePath.getTotalNumber()));
            counts = realtimeCheckService.updateOne(realtimeCheck);
            if(counts != 1) {
            	log.error("{}, 实时文件检测成功，但检测记录更新失败，info:{}",customerId,JSON.toJSONString(realtimeCheck));
            	dingDingMessage.sendMessage(String.format("警告：%s, 实时在线检测成功，但检测记录更新失败，realtimeId:%s", customerId,realtimeId));
            	return ;
            }
            
            CustomerConsume consume = new CustomerConsume();
            consume.setEmptyId(realtimeId)
                   .setCustomerId(customerId)
                   .setConsumeNumber(Long.valueOf(realtimeCvsFilePath.getTotalNumber()-(realtimeCvsFilePath.getUnknownNumber()
                		   												==null?0:realtimeCvsFilePath.getUnknownNumber())))
                   .setConsumeType(CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue())
                   .setClosingBalance(luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance())
                   .setOpeningBalance(consume.getClosingBalance() + consume.getConsumeNumber());
             counts = customerConsumeService.updateOne(consume);
             if(counts != 1) {
             	log.error("{}, 实时文件检测成功，但消耗记录更新失败，info:{}",customerId,JSON.toJSONString(consume));
             	dingDingMessage.sendMessage(String.format("警告：%s, 实时在线检测成功，但消耗记录更新失败，realtimeId:%s", customerId,realtimeId));
             	return ;
             }
            
            //设置程序运行结束
            redisClient.set(String.format(RealtimeRedisKeyConstant.THE_RUN_KEY, customerId, realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            //文件md5存入缓存
            fileUploadService.handleFileMd5Cache(realtimeId,customerId);
            log.info("----------用户编号：[{}]结束执行实时检测检索事件 ",customerId);            
        } catch (Exception e) {            
        	//返还预扣费
            balanceService.backDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),realtimeId);
            redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, customerId, realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            log.error("{},线程执行实时检测异常,realtimeId:{},info:",customerId,realtimeId,e);
            dingDingMessage.sendMessage(String.format("警告：%s,线程执行实时检测生成文件异常,realtimeId:%s,info:%s",customerId,realtimeId,e));
        }
    }
    
    private void realtimeFileDetection(List<String> mobileSubList,Long customerId,String fileUrl,Long realtimeId) throws Exception{
    	if(CollectionUtils.isEmpty(mobileSubList)) {
    		return ;
    	}
    	
   	   //查询检测接口
    	RealtimeFileDetectionResult detectionResult = realtimeFileDetectionNew(mobileSubList, customerId);        
       if (detectionResult != null && detectionResult.getData() != null) {
           ListMultimap<RealtimeReportGroupEnum, String> group = detectionResult.getData();
           //数据分组存入文本
           realtimeFileService.saveTestResultData(customerId, fileUrl, realtimeId, group);
       }else{
       	//大数据返回为空重试
       	log.error(">>>>>>>>>>>>>>> 实号文件检测异常，检测接口第一次调用返回为空， realtimeId:{}>>>>>>>>>>>>>>>",realtimeId);                                	
       	//根据检测状态码列表判断手机号状态
       	RealtimeFileDetectionResult reDetectionResult = realtimeFileDetectionNew(mobileSubList, customerId);  
       	if(reDetectionResult == null || reDetectionResult.getData() == null){
       		//第二次重试失败则这批号码直接放到沉默号
       		log.error(">>>>>>>>>>>>>>> 实时文件检测异常，检测接口第二次重试调用返回为空， realtimeId:{}>>>>>>>>>>>>>>>",realtimeId); 
       		realtimeFileService.saveMobileToTxtFile(fileUrl, mobileSubList, RealtimeTxtSuffixEnum.UNKNOWN);
       	}else{
           ListMultimap<RealtimeReportGroupEnum, String> group = reDetectionResult.getData();
           //数据分组存入文本
           realtimeFileService.saveTestResultData(customerId, fileUrl, realtimeId, group);
       	}                              	                                	
       }       
   }
    
    public RealtimeFileDetectionResult realtimeFileDetectionNew(List<String> mobileList,Long customerId) throws Exception {
    	RealtimeFileDetectionResult result = new RealtimeFileDetectionResult();
        ListMultimap<RealtimeReportGroupEnum, String> data = ArrayListMultimap.create();
 
        //请求api批量检查号码
        List<MobileRealtimeStatus> mobileRealtimeStatusList = asyncInvoke(mobileList, customerId);
        Set<String> hasResultSet = new HashSet<>();
        if(!CollectionUtils.isEmpty(mobileRealtimeStatusList)){
        	for (MobileRealtimeStatus mobileRealtimeStatus : mobileRealtimeStatusList) {
        		if(mobileRealtimeStatus == null) {
        			continue;
        		}
        		
                String m = mobileRealtimeStatus.getMobile();
                hasResultSet.add(m);
                //1.遍历状态
                RealtimeFileDetectionEnumAndDelivrdSet fileDetectionEnumAndDelivrdSet = realtimeFileDetectionDelivrdNew(m,mobileRealtimeStatus.getStatus(),mobileRealtimeStatus.getMnpStatus());
                if (fileDetectionEnumAndDelivrdSet != null) {
                    if (fileDetectionEnumAndDelivrdSet.getDetectedStatus() == 0) {
                        //设置分组
                    	RealtimeReportGroupEnum.FileDetection eFileDetectionEnum = fileDetectionEnumAndDelivrdSet.getFd();
                        //分组
                        if (eFileDetectionEnum != null) {
                            data.put(eFileDetectionEnum, m);
                            continue;
                        }
                    }
                }
                
                //2.如果都不包括，则分组为：没有返回结果
                data.put(RealtimeReportGroupEnum.FileDetection.UNKNOWN, m);
            }
        }
        
        if (!CollectionUtils.isEmpty(hasResultSet)) {
        	mobileList.removeAll(hasResultSet);
		}
        
        if (!CollectionUtils.isEmpty(mobileList)) {
			for(String mm : mobileList) {
				data.put(RealtimeReportGroupEnum.FileDetection.UNKNOWN, mm);
			}
		}
        
        result.setData(data);
        return result;
    }
    
    private List<MobileRealtimeStatus> asyncInvoke(List<String> mobileList,Long customerId){
    	// 按100个号码一批并发调用下游接口
    	List<List<String>> resultList = ListUtils.splitList(mobileList, 100);
    	List<MobileRealtimeStatus> result = new ArrayList<MobileRealtimeStatus>();
    	for(List<String> list : resultList) {
    		List<CompletableFuture<MobileRealtimeStatus>> cfts = new CopyOnWriteArrayList<>();
    		list.forEach(mobile->{
    			CompletableFuture<MobileRealtimeStatus> cft = CompletableFuture.supplyAsync(() -> {
    				MobileRealtimeStatus mobileRealtimeStatus = realtimeService.mobileStatusStaticQuery(customerId, mobile);
    				if (mobileRealtimeStatus ==null) {
    					return null;
    				}
    				
    				return mobileRealtimeStatus;
    			}, executor);
    			cfts.add(cft);
    		});
    		
    		Set<MobileRealtimeStatus> resultSet = cfts.parallelStream().map(futrue -> {
    			try {
    				return futrue.get(11, TimeUnit.SECONDS);
    			} catch (InterruptedException | ExecutionException | TimeoutException e) {
    				log.error("{}, 实时在线检测并发调用异常，info:",customerId,e);
    				return null;
    			}
    		}).collect(Collectors.toSet());
    		
    		result.addAll(resultSet);
    	}
    	
		return result;
    }
    
	private RealtimeFileDetectionEnumAndDelivrdSet realtimeFileDetectionDelivrdNew(String mobile,String status,String mnpStatus) {
		RealtimeFileDetectionEnumAndDelivrdSet result = new RealtimeFileDetectionEnumAndDelivrdSet();
		//检测结果  0：有正常结果   1：状态未知   2：没有结果
		result.setDetectedStatus(2);
		if (StringUtils.isNotBlank(status)) {
			if(CommonConstant.REALTIME_NORMAL_STATUS.equals(status)) {
				if(CommonConstant.REALTIME_MNP_STATUS.equals(mnpStatus)) {
					result.setFd(RealtimeReportGroupEnum.FileDetection.MNP);
				}else {
					result.setFd(RealtimeReportGroupEnum.FileDetection.NORMAL);
				}
				
				result.setDetectedStatus(0);
				return result;
			}else if(new ArrayList<String>(Arrays.asList(CommonConstant.REALTIME_FREE_CHARGE_CODE.split(","))).contains(status)) {
				result.setFd(RealtimeReportGroupEnum.FileDetection.UNKNOWN);
				result.setDetectedStatus(0);
				return result;
			}else {
				//文件检测分组枚举
				RealtimeReportGroupEnum.FileDetection fileDetectionEnum = RealtimeReportGroupEnum.FileDetection.fromBackGroupCode(status);
				if (fileDetectionEnum != null) {
					result.setFd(fileDetectionEnum);
					result.setDetectedStatus(0);
					return result;
				}
			}
		}
		
		result.setFd(RealtimeReportGroupEnum.FileDetection.UNKNOWN);
		return result;
	}
}

package com.zhongzhi.empty.service.international;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ListMultimap;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.constants.InternationalRedisKeyConstant;
import com.zhongzhi.empty.entity.Customer;
import com.zhongzhi.empty.entity.CustomerConsume;
import com.zhongzhi.empty.entity.InternationalCheck;
import com.zhongzhi.empty.entity.InternationalCvsFilePath;
import com.zhongzhi.empty.entity.InternationalRunTestDomian;
import com.zhongzhi.empty.entity.TxtFileContent;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.enums.InternationalMobileReportGroupEnum;
import com.zhongzhi.empty.enums.InternationalTxtSuffixEnum;
import com.zhongzhi.empty.enums.ProductEnum;
import com.zhongzhi.empty.redis.DistributedLockWrapper;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.LuaByDeductFeeResponse;
import com.zhongzhi.empty.service.ThreadExecutorService;
import com.zhongzhi.empty.service.balance.BalanceService;
import com.zhongzhi.empty.service.customer.CustomerConsumeService;
import com.zhongzhi.empty.service.customer.CustomerService;
import com.zhongzhi.empty.service.direct.FileInternationalCheckService;
import com.zhongzhi.empty.service.file.FileRedisService;
import com.zhongzhi.empty.service.file.FileUploadService;
import com.zhongzhi.empty.service.gateway.InternationalService;
import com.zhongzhi.empty.util.DingDingMessage;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 国际检测新版本实现类
 * @author liuh
 * @date 2022年10月15日
 */
@Slf4j
@Service
public class FileInternationalCheckNewService {

	@Autowired
	private JedisPool jedisPool;

	@Autowired
	private FileRedisService fileRedisService;

	@Autowired
	private RedisClient redisClient;

	@Autowired
	private BalanceService balanceService;

	@Autowired
	private DingDingMessage dingDingMessage;

	@Autowired
	private InternationalFileService fileService;

	@Autowired
	private InternationalService internationalService;

	@Autowired
	private CustomerConsumeService customerConsumeService;

	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private InternationalCheckService internationalCheckService;
	
	@Autowired
	private InternationalCvsFilePathService internationalCvsFilePathService;
	
	@Autowired
	private FileUploadService fileUploadService;
	
	@Autowired
    private ThreadExecutorService threadExecutorService;
	
	@Autowired
	private FileInternationalCheckService fileInternationalCheckService;
	
	private static final int BATCH_NUM_SIZE = 2000;
	
	private static ExecutorService executor = Executors.newFixedThreadPool(10);

	public ApiResult executeInternationalCheck(Long customerId, Long internationalId, Long totalNumber,
			String sourceFileName, String uploadPath, String countryCode,String productType) {
		if(StringUtils.isNotBlank(productType)) {
			return fileInternationalCheckService.executeIntDirectCheck(customerId, internationalId, 
					totalNumber, sourceFileName, uploadPath, countryCode,productType);
		}
		
		return fileInternationalCheckService.executeInternationalCheck(customerId, internationalId, totalNumber, sourceFileName, uploadPath, countryCode, productType);		
	}
	
	private ApiResult internationalMobileDetection(Long customerId, Long internationalId, Long totalNumber,
			String sourceFileName, String uploadPath, String countryCode,String productType) {
		Customer customer = customerService.getCustomerById(customerId);
		if (customer == null) {
			return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "账号不存在", null);
		}

		int expire = 3 * 60 * 60 * 1000; // 超时时间
		int mobileCount = 0;
		String sendID = "";
		// 1.设置redis锁
		DistributedLockWrapper lock = new DistributedLockWrapper(jedisPool,
				String.format(InternationalRedisKeyConstant.THE_TEST_FUN_KEY, customerId, internationalId),
				1000L * 60 * 60, expire);
		if (StringUtils.isBlank(lock.getIdentifier())) {
			log.error("{}, 该文件已提交检测，internationalId:{}", customerId, internationalId);
			return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "已提交检测，请勿重复提交", null);
		}

		log.info("----------用户[{}]开始执行国际检测检索事件,internationalId：{}", customerId, internationalId);
		//2.初始化国际检测相关配置redis值
		fileRedisService.internationalRedisInit(customerId, internationalId, expire, lock.getIdentifier(),
				Long.valueOf(mobileCount));
		//3.获取有效的手机号码以及文件编码格式
    	TxtFileContent fileContent = fileService.getValidMobileListByInternationalTxt(uploadPath);
        if (fileContent.getMobileCounts() <= 0) {
        	lock.releaseLock();
            redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,internationalId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            return ApiResult.result(ApiCode.COUNT_EXCEPTION, "待检号码统计为0或统计失败", null);
        }

		mobileCount = Integer.valueOf(fileContent.getMobileCounts());
		//4.冻结账户余额
		Boolean isFreeze = balanceService.preDeductFee(customerId, mobileCount,
				ProductEnum.INTERNATIONAL.getProductCode(), internationalId);
		if (!isFreeze) {
			lock.releaseLock();
			redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId),
					CommonConstant.FILE_TEST_FAILED_CODE, expire);
			return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足或预扣失败", null);
		}

		//5.提交的有效号码总个数存入redis
		String addFlag = redisClient.set(String.format(InternationalRedisKeyConstant.SUCCEED_CLEARING_COUNT_KEY,
				customerId, internationalId), String.valueOf(mobileCount), expire);
		if (StringUtils.isBlank(addFlag) || !CommonConstant.REDIS_SET_RETURN.equals(addFlag)) {
			lock.releaseLock();
			redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId),
					CommonConstant.FILE_TEST_FAILED_CODE, expire);
			// 返还预扣费
			balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),
					internationalId);
			return ApiResult.result(ApiCode.COUNT_EXCEPTION, "计数异常", null);
		}

		try {
	        //6.保存临时文件
	        fileService.saveTempFileByAll(uploadPath, customerId, fileContent.getMobileList(), internationalId);
	        //7.执行检测
	        internationalFileDetectionByTxt(mobileCount, customerId, expire, lock, uploadPath,
	        		internationalId, fileContent.getFileCode(),sourceFileName,fileContent.getErrorCounts(),countryCode);
        } catch (IOException e) {
        	lock.releaseLock();
			// 返还预扣费
			balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),
					internationalId);

			log.error("{}, 用户国际在线检测异常，internationalId:{},info:", customerId, internationalId, e);
			dingDingMessage.sendMessage(String.format("警告： 用户【%s】国际在线检测生成临时文件异常，internationalId:%s，info:%s", customerId,
					internationalId, e));
			return ApiResult.fail("国际检测异常");
		}
        
		lock.releaseLock();
		return ApiResult.ok(new InternationalRunTestDomian(mobileCount, CommonConstant.THETEST_RUNNING,
				internationalId.toString(), sendID));
	}
	
	private Future<?> internationalFileDetectionByTxt(int mobileCount, Long customerId, Integer expire,
			DistributedLockWrapper lock, String fileUrl, Long internationalId, String fileEncoding, String sourceFileName,
			Integer errorCounts, String countryCode) {
		Runnable run = new Runnable() {
			public void run() {
				Jedis jedis = null;
				try {
					jedis = jedisPool.getResource();
					//出现异常终止检测
					String exceptions = jedis.get(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId));
					if (CommonConstant.FILE_TESTING_CODE.equals(exceptions)) {
						long beginTime = System.currentTimeMillis();
						//新建检查进度记录
						log.info(">>>>>>>>>>>>>>> 国际文件开始检查 internationalId:{}，数量:{}>>>>>>>>>>>>>>>", internationalId,mobileCount);
						//批次数    分批检测手机号状态，每次2000个
						int batchCount = mobileCount / BATCH_NUM_SIZE;
						for (int i = 0; i < (batchCount + 1); i++) {
							//检测的号码标志位
							int fromIndex = BATCH_NUM_SIZE * i + 1;
							int toIndex = (fromIndex + BATCH_NUM_SIZE - 1) > mobileCount ? mobileCount
									: (fromIndex + BATCH_NUM_SIZE - 1);
							//调用国际检测接口
							internationalFileDetection(fileService.readTxtFileContent(fileUrl, fileEncoding, fromIndex),
									customerId, fileUrl, internationalId,countryCode);
							//等待1.2秒
							Thread.sleep(20);
							log.info(">>>>>>>>>>>>>>> 国际文件检测中， internationalId:{}，当前进度:{}/{}>>>>>>>>>>>>>>>", internationalId,toIndex, mobileCount);
							// 成功检测条数累加
							redisClient.set(String.format(InternationalRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, customerId,internationalId), String.valueOf(toIndex), expire);
						}
						//完成后等待30秒
						Thread.sleep((new Random().nextInt(20000)) + 5000);
						log.info(">>>>>>>>>>>>>>> 国际文件结束检查，等待生成txt，internationalId:{}，数量:{}，用时:{} >>>>>>>>>>>>>>>",
								internationalId, mobileCount, (System.currentTimeMillis() - beginTime));
						//生成结果文件
						generateResultFiles(fileUrl, customerId, internationalId, sourceFileName, mobileCount, expire,
								errorCounts);
						log.info(">>>>>>>>>>>>>>> 国际文件完成任务，internationalId:{}，数量:{}，用时:{} >>>>>>>>>>>>>>>", internationalId,
								mobileCount, (System.currentTimeMillis() - beginTime));
					}
				} catch (Exception e) {
					log.error("{}, 线程执行国际检测异常, internationalId:{},info:", customerId, internationalId, e);
					dingDingMessage.sendMessage(
							String.format("警告：%s,执行国际检测出现系统异常,internationalId:%s,info:%s", customerId, internationalId, e));
					//返还预扣费
					balanceService.backDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),
							internationalId);
				} finally {
					// 返还到连接池
					jedis.close();
				}
			}
		};

		// 加入线程池开始执行
		return threadExecutorService.execute(run);
	}
	
	@Transactional
    private void generateResultFiles(String fileUrl, Long customerId, Long internationalId, String sourceFileName,int mobileCount,Integer expire,Integer errorCounts) {
        try {
            //获取检测结果
        	InternationalCvsFilePath internationalCvsFilePath = fileService.getTestResultByTxtFile(fileUrl, customerId, internationalId, sourceFileName);          
            if (internationalCvsFilePath.getTotalNumber() == 0 ) {
            	//返还预扣费
                balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),internationalId);
                redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
                return;
            }
                    	
            int counts = internationalCvsFilePathService.saveOne(internationalCvsFilePath);
            if(counts != 1) {
            	log.error("{}, 国际文件检测成功，但文件结果包信息记录插入失败，info:{}",customerId,JSON.toJSONString(internationalCvsFilePath));
            	dingDingMessage.sendMessage(String.format("警告：%s, 国际在线检测成功，但文件结果包信息记录插入失败，internationalId:%s", customerId,internationalId));
            	return ;
            }
            
            //执行扣费操作
            LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerId, mobileCount, 
            		internationalCvsFilePath.getTotalNumber(), ProductEnum.INTERNATIONAL.getProductCode(),internationalId);
            
            // 将国际检测查询接口返回的检测数据保存到数据库，同时更新国际检测订单状态
            InternationalCheck internationalCheck = new InternationalCheck();
    		internationalCheck.setId(internationalId).setStatus(InternationalCheck.InternationalCheckStatus.WORK_FINISH.getStatus())
    				.setActiveCount(Long.valueOf(internationalCvsFilePath.getActiveNumber()==null?0:internationalCvsFilePath.getActiveNumber()))
    				.setNoRegisterCount(Long.valueOf(internationalCvsFilePath.getNoRegisterNumber()==null?0:internationalCvsFilePath.getNoRegisterNumber()))
    				.setUnknownCount(Long.valueOf(internationalCvsFilePath.getUnknownNumber()==null?0:internationalCvsFilePath.getUnknownNumber()))
    				.setIllegalNumber(Long.valueOf(errorCounts))
    				.setTotalNumber(Long.valueOf(internationalCvsFilePath.getTotalNumber()==null?0:internationalCvsFilePath.getTotalNumber()));
            
            counts = internationalCheckService.updateOne(internationalCheck);
            if(counts != 1) {
            	log.error("{}, 国际文件检测成功，但检测记录更新失败，info:{}",customerId,JSON.toJSONString(internationalCheck));
            	dingDingMessage.sendMessage(String.format("警告：%s, 国际在线检测成功，但检测记录更新失败，internationalId:%s", customerId,internationalId));
            	return ;
            }
            
            CustomerConsume consume = new CustomerConsume();
            consume.setEmptyId(internationalId)
                   .setCustomerId(customerId)
                   .setConsumeNumber(Long.valueOf(internationalCvsFilePath.getTotalNumber()))
                   .setConsumeType(CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue())
                   .setClosingBalance(luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance())
                   .setOpeningBalance(consume.getClosingBalance() + consume.getConsumeNumber());
             counts = customerConsumeService.updateOne(consume);
             if(counts != 1) {
             	log.error("{}, 国际文件检测成功，但消耗记录更新失败，info:{}",customerId,JSON.toJSONString(consume));
             	dingDingMessage.sendMessage(String.format("警告：%s, 国际在线检测成功，但消耗记录更新失败，realtimeId:%s", customerId,internationalId));
             	return ;
             }
            
            //设置程序运行结束
            redisClient.set(String.format(InternationalRedisKeyConstant.THE_RUN_KEY, customerId, internationalId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            //文件md5存入缓存
            fileUploadService.handleFileMd5Cache(internationalId,customerId);
            log.info("----------用户编号：[{}]结束执行国际检测检索事件 ,internationalId:{}",customerId,internationalId);            
        } catch (Exception e) {            
        	//返还预扣费
            balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),internationalId);
            redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            log.error("{},线程执行国际检测异常,internationalId:{},info:",customerId,internationalId,e);
            dingDingMessage.sendMessage(String.format("警告：%s,线程执行国际检测生成文件异常,realtimeId:%s,info:%s",customerId,internationalId,e));
        }
    }
	
	private void internationalFileDetection(List<String> mobileSubList,Long customerId,String fileUrl,Long internationalId, String countryCode) throws Exception{
    	if(CollectionUtils.isEmpty(mobileSubList)) {
    		return ;
    	}
    	
    	//查询大数据接口
    	ListMultimap<InternationalMobileReportGroupEnum,String> detectionResult =
        		internationalService.internationalFileDetection(mobileSubList,customerId, countryCode);       
        if (detectionResult != null) {
           //数据分组存入文本
           fileService.saveTestResultData(customerId, fileUrl, internationalId, detectionResult);
       }else{
       	//大数据返回为空重试
       	log.error(">>>>>>>>>>>>>>> 国际文件检测异常，检测接口第一次调用返回为空， internationalId:{}>>>>>>>>>>>>>>>",internationalId);                                	
       	//根据检测状态码列表判断手机号状态
       	ListMultimap<InternationalMobileReportGroupEnum,String> reDetectionResult =
        		internationalService.internationalFileDetection(mobileSubList,customerId, countryCode);  
       	if(reDetectionResult == null){
       		//第二次重试失败则这批号码直接放到未知
       		log.error(">>>>>>>>>>>>>>> 国际文件检测异常，检测接口第二次重试调用返回为空， internationalId:{}>>>>>>>>>>>>>>>",internationalId); 
       		fileService.saveMobileToTxtFile(fileUrl, mobileSubList, InternationalTxtSuffixEnum.UNKNOWN);
       	}else{
           //数据分组存入文本
       		fileService.saveTestResultData(customerId, fileUrl, internationalId, reDetectionResult);
       	}                              	                                	
       }       
   }
}

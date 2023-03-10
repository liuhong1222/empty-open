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
 * ??????????????????????????????
 * @author liuh
 * @date 2022???10???15???
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
		
		Customer customer = customerService.getCustomerById(customerId);
		if (customer == null) {
			return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "???????????????", null);
		}

		int expire = 3 * 60 * 60 * 1000; // ????????????
		int mobileCount = 0;
		String sendID = "";
		// 1.??????redis???
		DistributedLockWrapper lock = new DistributedLockWrapper(jedisPool,
				String.format(InternationalRedisKeyConstant.THE_TEST_FUN_KEY, customerId, internationalId),
				1000L * 60 * 60, expire);
		if (StringUtils.isBlank(lock.getIdentifier())) {
			log.error("{}, ???????????????????????????internationalId:{}", customerId, internationalId);
			return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "????????????????????????????????????", null);
		}

		log.info("----------??????[{}]????????????????????????????????????,internationalId???{}", customerId, internationalId);
		//2.?????????????????????????????????redis???
		fileRedisService.internationalRedisInit(customerId, internationalId, expire, lock.getIdentifier(),
				Long.valueOf(mobileCount));
		//3.???????????????????????????????????????????????????
    	TxtFileContent fileContent = fileService.getValidMobileListByInternationalTxt(uploadPath);
        if (fileContent.getMobileCounts() <= 0) {
        	lock.releaseLock();
            redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,internationalId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            return ApiResult.result(ApiCode.COUNT_EXCEPTION, "?????????????????????0???????????????", null);
        }

		mobileCount = Integer.valueOf(fileContent.getMobileCounts());
		//4.??????????????????
		Boolean isFreeze = balanceService.preDeductFee(customerId, mobileCount,
				ProductEnum.INTERNATIONAL.getProductCode(), internationalId);
		if (!isFreeze) {
			lock.releaseLock();
			redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId),
					CommonConstant.FILE_TEST_FAILED_CODE, expire);
			return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "???????????????????????????", null);
		}

		//5.????????????????????????????????????redis
		String addFlag = redisClient.set(String.format(InternationalRedisKeyConstant.SUCCEED_CLEARING_COUNT_KEY,
				customerId, internationalId), String.valueOf(mobileCount), expire);
		if (StringUtils.isBlank(addFlag) || !CommonConstant.REDIS_SET_RETURN.equals(addFlag)) {
			lock.releaseLock();
			redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId),
					CommonConstant.FILE_TEST_FAILED_CODE, expire);
			// ???????????????
			balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),
					internationalId);
			return ApiResult.result(ApiCode.COUNT_EXCEPTION, "????????????", null);
		}

		try {
	        //6.??????????????????
	        fileService.saveTempFileByAll(uploadPath, customerId, fileContent.getMobileList(), internationalId);
	        //7.????????????
	        internationalFileDetectionByTxt(mobileCount, customerId, expire, lock, uploadPath,
	        		internationalId, fileContent.getFileCode(),sourceFileName,fileContent.getErrorCounts(),countryCode);
        } catch (IOException e) {
        	lock.releaseLock();
			// ???????????????
			balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),
					internationalId);

			log.error("{}, ?????????????????????????????????internationalId:{},info:", customerId, internationalId, e);
			dingDingMessage.sendMessage(String.format("????????? ?????????%s????????????????????????????????????????????????internationalId:%s???info:%s", customerId,
					internationalId, e));
			return ApiResult.fail("??????????????????");
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
					//????????????????????????
					String exceptions = jedis.get(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId));
					if (CommonConstant.FILE_TESTING_CODE.equals(exceptions)) {
						long beginTime = System.currentTimeMillis();
						//????????????????????????
						log.info(">>>>>>>>>>>>>>> ???????????????????????? internationalId:{}?????????:{}>>>>>>>>>>>>>>>", internationalId,mobileCount);
						//?????????    ????????????????????????????????????2000???
						int batchCount = mobileCount / BATCH_NUM_SIZE;
						for (int i = 0; i < (batchCount + 1); i++) {
							//????????????????????????
							int fromIndex = BATCH_NUM_SIZE * i + 1;
							int toIndex = (fromIndex + BATCH_NUM_SIZE - 1) > mobileCount ? mobileCount
									: (fromIndex + BATCH_NUM_SIZE - 1);
							//????????????????????????
							internationalFileDetection(fileService.readTxtFileContent(fileUrl, fileEncoding, fromIndex),
									customerId, fileUrl, internationalId,countryCode);
							//??????1.2???
							Thread.sleep(20);
							log.info(">>>>>>>>>>>>>>> ???????????????????????? internationalId:{}???????????????:{}/{}>>>>>>>>>>>>>>>", internationalId,toIndex, mobileCount);
							// ????????????????????????
							redisClient.set(String.format(InternationalRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, customerId,internationalId), String.valueOf(toIndex), expire);
						}
						//???????????????30???
						Thread.sleep((new Random().nextInt(20000)) + 5000);
						log.info(">>>>>>>>>>>>>>> ???????????????????????????????????????txt???internationalId:{}?????????:{}?????????:{} >>>>>>>>>>>>>>>",
								internationalId, mobileCount, (System.currentTimeMillis() - beginTime));
						//??????????????????
						generateResultFiles(fileUrl, customerId, internationalId, sourceFileName, mobileCount, expire,
								errorCounts);
						log.info(">>>>>>>>>>>>>>> ???????????????????????????internationalId:{}?????????:{}?????????:{} >>>>>>>>>>>>>>>", internationalId,
								mobileCount, (System.currentTimeMillis() - beginTime));
					}
				} catch (Exception e) {
					log.error("{}, ??????????????????????????????, internationalId:{},info:", customerId, internationalId, e);
					dingDingMessage.sendMessage(
							String.format("?????????%s,????????????????????????????????????,internationalId:%s,info:%s", customerId, internationalId, e));
					//???????????????
					balanceService.backDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),
							internationalId);
				} finally {
					// ??????????????????
					jedis.close();
				}
			}
		};

		// ???????????????????????????
		return threadExecutorService.execute(run);
	}
	
	@Transactional
    private void generateResultFiles(String fileUrl, Long customerId, Long internationalId, String sourceFileName,int mobileCount,Integer expire,Integer errorCounts) {
        try {
            //??????????????????
        	InternationalCvsFilePath internationalCvsFilePath = fileService.getTestResultByTxtFile(fileUrl, customerId, internationalId, sourceFileName);          
            if (internationalCvsFilePath.getTotalNumber() == 0 ) {
            	//???????????????
                balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),internationalId);
                redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
                return;
            }
                    	
            int counts = internationalCvsFilePathService.saveOne(internationalCvsFilePath);
            if(counts != 1) {
            	log.error("{}, ????????????????????????????????????????????????????????????????????????info:{}",customerId,JSON.toJSONString(internationalCvsFilePath));
            	dingDingMessage.sendMessage(String.format("?????????%s, ????????????????????????????????????????????????????????????????????????internationalId:%s", customerId,internationalId));
            	return ;
            }
            
            //??????????????????
            LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerId, mobileCount, 
            		internationalCvsFilePath.getTotalNumber(), ProductEnum.INTERNATIONAL.getProductCode(),internationalId);
            
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????
            InternationalCheck internationalCheck = new InternationalCheck();
    		internationalCheck.setId(internationalId).setStatus(InternationalCheck.InternationalCheckStatus.WORK_FINISH.getStatus())
    				.setActiveCount(Long.valueOf(internationalCvsFilePath.getActiveNumber()==null?0:internationalCvsFilePath.getActiveNumber()))
    				.setNoRegisterCount(Long.valueOf(internationalCvsFilePath.getNoRegisterNumber()==null?0:internationalCvsFilePath.getNoRegisterNumber()))
    				.setUnknownCount(Long.valueOf(internationalCvsFilePath.getUnknownNumber()==null?0:internationalCvsFilePath.getUnknownNumber()))
    				.setIllegalNumber(Long.valueOf(errorCounts))
    				.setTotalNumber(Long.valueOf(internationalCvsFilePath.getTotalNumber()==null?0:internationalCvsFilePath.getTotalNumber()));
            
            counts = internationalCheckService.updateOne(internationalCheck);
            if(counts != 1) {
            	log.error("{}, ?????????????????????????????????????????????????????????info:{}",customerId,JSON.toJSONString(internationalCheck));
            	dingDingMessage.sendMessage(String.format("?????????%s, ?????????????????????????????????????????????????????????internationalId:%s", customerId,internationalId));
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
             	log.error("{}, ?????????????????????????????????????????????????????????info:{}",customerId,JSON.toJSONString(consume));
             	dingDingMessage.sendMessage(String.format("?????????%s, ?????????????????????????????????????????????????????????realtimeId:%s", customerId,internationalId));
             	return ;
             }
            
            //????????????????????????
            redisClient.set(String.format(InternationalRedisKeyConstant.THE_RUN_KEY, customerId, internationalId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            //??????md5????????????
            fileUploadService.handleFileMd5Cache(internationalId,customerId);
            log.info("----------???????????????[{}]???????????????????????????????????? ,internationalId:{}",customerId,internationalId);            
        } catch (Exception e) {            
        	//???????????????
            balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),internationalId);
            redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            log.error("{},??????????????????????????????,internationalId:{},info:",customerId,internationalId,e);
            dingDingMessage.sendMessage(String.format("?????????%s,??????????????????????????????????????????,realtimeId:%s,info:%s",customerId,internationalId,e));
        }
    }
	
	private void internationalFileDetection(List<String> mobileSubList,Long customerId,String fileUrl,Long internationalId, String countryCode) throws Exception{
    	if(CollectionUtils.isEmpty(mobileSubList)) {
    		return ;
    	}
    	
    	//?????????????????????
    	ListMultimap<InternationalMobileReportGroupEnum,String> detectionResult =
        		internationalService.internationalFileDetection(mobileSubList,customerId, countryCode);       
        if (detectionResult != null) {
           //????????????????????????
           fileService.saveTestResultData(customerId, fileUrl, internationalId, detectionResult);
       }else{
       	//???????????????????????????
       	log.error(">>>>>>>>>>>>>>> ????????????????????????????????????????????????????????????????????? internationalId:{}>>>>>>>>>>>>>>>",internationalId);                                	
       	//????????????????????????????????????????????????
       	ListMultimap<InternationalMobileReportGroupEnum,String> reDetectionResult =
        		internationalService.internationalFileDetection(mobileSubList,customerId, countryCode);  
       	if(reDetectionResult == null){
       		//??????????????????????????????????????????????????????
       		log.error(">>>>>>>>>>>>>>> ??????????????????????????????????????????????????????????????????????????? internationalId:{}>>>>>>>>>>>>>>>",internationalId); 
       		fileService.saveMobileToTxtFile(fileUrl, mobileSubList, InternationalTxtSuffixEnum.UNKNOWN);
       	}else{
           //????????????????????????
       		fileService.saveTestResultData(customerId, fileUrl, internationalId, reDetectionResult);
       	}                              	                                	
       }       
   }
}

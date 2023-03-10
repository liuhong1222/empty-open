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
 * ??????????????????
 * @author liuh
 * @date 2021???11???3???
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
		int expire = 3 * 60 * 60 * 1000; //????????????
        //1.??????redis???
        DistributedLockWrapper lock = new DistributedLockWrapper(jedisPool, String.format(RealtimeRedisKeyConstant.THE_TEST_FUN_KEY, 
        		customerId,realtimeId), 1000L * 60 * 60, expire);
        if (StringUtils.isBlank(lock.getIdentifier())) {
        	log.error("{}, ???????????????????????????emptyId:{}",customerId,realtimeId);
        	return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "????????????????????????????????????", null);
        }
        
    	log.info("----------??????[{}]????????????????????????????????????,emptyId???{}",customerId,realtimeId);
    	//2.?????????????????????????????????redis???
    	fileRedisService.realtimeRedisInit(customerId, realtimeId, expire, lock.getIdentifier(),totalNumber);             
        //3.???????????????????????????????????????????????????
    	TxtFileContent fileContent = realtimeFileService.getValidMobileListByTxt(uploadPath);
        List<String> mobileList = fileContent.getMobileList();                
        if (CollectionUtils.isEmpty(mobileList)) {
        	lock.releaseLock();
            redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,customerId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            return ApiResult.result(ApiCode.COUNT_EXCEPTION, "?????????????????????0???????????????", null);
        }
        
        int mobileCount = mobileList.size();
        //4.??????????????????
        Boolean isFreeze = balanceService.preDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),realtimeId);
        if (!isFreeze) {
        	lock.releaseLock();
            redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "???????????????????????????", null);
        }
        
        //5.????????????????????????????????????redis
        String addFlag = redisClient.set(String.format(RealtimeRedisKeyConstant.SUCCEED_CLEARING_COUNT_KEY, 
        								customerId,realtimeId),String.valueOf(mobileCount),expire);
        if(StringUtils.isBlank(addFlag) || !CommonConstant.REDIS_SET_RETURN.equals(addFlag)) {
        	lock.releaseLock();
            redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, 
            		customerId,realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            //???????????????
            balanceService.backDeductFee(customerId, mobileCount,ProductEnum.REALTIME.getProductCode(),realtimeId);
            return ApiResult.result(ApiCode.COUNT_EXCEPTION, "????????????", null);
        }
        
        try {
	        //6.??????????????????
        	realtimeFileService.saveTempFileByAll(uploadPath, customerId, mobileList, realtimeId);
	        //7.????????????
	        realtimeFileDetectionByTxtNew(mobileCount, customerId, expire, lock, uploadPath,
	        									realtimeId, fileContent.getFileCode(),sourceFileName,fileContent.getErrorCounts());
        } catch (IOException e) {
			lock.releaseLock();
			//???????????????
            balanceService.backDeductFee(customerId, mobileCount,ProductEnum.REALTIME.getProductCode(),realtimeId);
            
			log.error("{}, ?????????????????????????????????emptyId:{},info:", customerId,realtimeId,e);
			dingDingMessage.sendMessage(String.format("????????? ?????????%s????????????????????????????????????????????????emptyId:%s???info:%s", customerId,realtimeId,e));
			return ApiResult.fail("??????????????????");
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
     * ??????????????????????????????????????????
     */
    private Future<?> realtimeFileDetectionByTxtNew(int mobileCount, Long customerId,
                                                              Integer expire,DistributedLockWrapper lock,
                                                              String fileUrl,Long realtimeId, String fileEncoding,String sourceFileName,Integer errorCounts) {
    	Runnable run = new Runnable() {
			public void run() {
				Jedis jedis = null;
                try {
                    jedis = jedisPool.getResource();
                    //????????????????????????
                    String exceptions = jedis.get(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, customerId,realtimeId));
                    if (CommonConstant.FILE_TESTING_CODE.equals(exceptions)) {                      	
                        long beginTime = System.currentTimeMillis();
                        //????????????????????????
                        log.info(">>>>>>>>>>>>>>> ???????????????????????? realtimeId:{}?????????:{}>>>>>>>>>>>>>>>",realtimeId,mobileCount);                       
                        //?????????    ????????????????????????????????????2000???
                        int batchCount = mobileCount / BATCH_NUM_SIZE;
                        for (int i = 0; i < (batchCount + 1); i++) {                            
                            //????????????????????????
                            int fromIndex = BATCH_NUM_SIZE * i + 1;
                            int toIndex = (fromIndex + BATCH_NUM_SIZE - 1)>mobileCount?mobileCount:(fromIndex + BATCH_NUM_SIZE - 1);
                            //????????????????????????
                            realtimeFileDetection(realtimeFileService.readTxtFileContent(fileUrl, fileEncoding, fromIndex),customerId,fileUrl,realtimeId);                            
                            //??????1.2???
                            Thread.sleep(200);
                            log.info(">>>>>>>>>>>>>>> ???????????????????????? realtimeId:{}???????????????:{}/{}>>>>>>>>>>>>>>>",realtimeId,toIndex,mobileCount);
                            // ????????????????????????
                            redisClient.set(String.format(RealtimeRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, customerId,realtimeId), String.valueOf(toIndex), expire);
                        }
                        //???????????????30???
                        Thread.sleep((new Random().nextInt(20000)) + 5000);
                        log.info(">>>>>>>>>>>>>>> ???????????????????????????????????????txt???realtimeId:{}?????????:{}?????????:{} >>>>>>>>>>>>>>>",realtimeId,mobileCount,(System.currentTimeMillis() - beginTime));
                        //??????????????????
                        generateResultFiles(fileUrl, customerId, realtimeId, sourceFileName, mobileCount, expire,errorCounts);
                        log.info(">>>>>>>>>>>>>>> ???????????????????????????realtimeId:{}?????????:{}?????????:{} >>>>>>>>>>>>>>>",realtimeId,mobileCount,(System.currentTimeMillis() - beginTime));
                    }
                } catch (Exception e) {
                    log.error("{}, ??????????????????????????????, realtimeId:{},info:",customerId,realtimeId, e);
                    dingDingMessage.sendMessage(String.format("?????????%s,????????????????????????????????????,realtimeId:%s,info:%s", customerId,realtimeId,e));
                    //???????????????
                    balanceService.backDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),realtimeId);
                } finally {
                    // ??????????????????
                    jedis.close();
                }
			}
		};
		
		// ???????????????????????????
        return threadExecutorService.execute(run);
    }
    
    /**
     * ??????????????????
     */
    @Transactional
    private void generateResultFiles(String fileUrl, Long customerId, Long realtimeId, String sourceFileName,int mobileCount,Integer expire,Integer errorCounts) {
        try {
            //??????????????????
        	RealtimeResultData realtimeResultData = realtimeFileService.getTestResultByTxtFile(fileUrl, customerId, realtimeId, sourceFileName);          
            if (realtimeResultData.getTotalCount() == 0 ) {
            	//???????????????
                balanceService.backDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),realtimeId);
                redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, customerId, realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
                return;
            }
                    	
            RealtimeCvsFilePath realtimeCvsFilePath = realtimeResultData.getRealtimeCvsFilePath();
            int counts = realtimeCvsFilePathService.saveOne(realtimeCvsFilePath);
            if(counts != 1) {
            	log.error("{}, ????????????????????????????????????????????????????????????????????????info:{}",customerId,JSON.toJSONString(realtimeCvsFilePath));
            	dingDingMessage.sendMessage(String.format("?????????%s, ????????????????????????????????????????????????????????????????????????emptyId:%s", customerId,customerId));
            	return ;
            }
            
            //??????????????????
            LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerId, mobileCount, 
            		realtimeCvsFilePath.getTotalNumber()-(realtimeCvsFilePath.getUnknownNumber()==null?0:realtimeCvsFilePath.getUnknownNumber()), ProductEnum.REALTIME.getProductCode(),realtimeId);
            
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????
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
            	log.error("{}, ?????????????????????????????????????????????????????????info:{}",customerId,JSON.toJSONString(realtimeCheck));
            	dingDingMessage.sendMessage(String.format("?????????%s, ?????????????????????????????????????????????????????????realtimeId:%s", customerId,realtimeId));
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
             	log.error("{}, ?????????????????????????????????????????????????????????info:{}",customerId,JSON.toJSONString(consume));
             	dingDingMessage.sendMessage(String.format("?????????%s, ?????????????????????????????????????????????????????????realtimeId:%s", customerId,realtimeId));
             	return ;
             }
            
            //????????????????????????
            redisClient.set(String.format(RealtimeRedisKeyConstant.THE_RUN_KEY, customerId, realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            //??????md5????????????
            fileUploadService.handleFileMd5Cache(realtimeId,customerId);
            log.info("----------???????????????[{}]???????????????????????????????????? ",customerId);            
        } catch (Exception e) {            
        	//???????????????
            balanceService.backDeductFee(customerId, mobileCount, ProductEnum.REALTIME.getProductCode(),realtimeId);
            redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, customerId, realtimeId), CommonConstant.FILE_TEST_FAILED_CODE, expire);
            log.error("{},??????????????????????????????,realtimeId:{},info:",customerId,realtimeId,e);
            dingDingMessage.sendMessage(String.format("?????????%s,??????????????????????????????????????????,realtimeId:%s,info:%s",customerId,realtimeId,e));
        }
    }
    
    private void realtimeFileDetection(List<String> mobileSubList,Long customerId,String fileUrl,Long realtimeId) throws Exception{
    	if(CollectionUtils.isEmpty(mobileSubList)) {
    		return ;
    	}
    	
   	   //??????????????????
    	RealtimeFileDetectionResult detectionResult = realtimeFileDetectionNew(mobileSubList, customerId);        
       if (detectionResult != null && detectionResult.getData() != null) {
           ListMultimap<RealtimeReportGroupEnum, String> group = detectionResult.getData();
           //????????????????????????
           realtimeFileService.saveTestResultData(customerId, fileUrl, realtimeId, group);
       }else{
       	//???????????????????????????
       	log.error(">>>>>>>>>>>>>>> ????????????????????????????????????????????????????????????????????? realtimeId:{}>>>>>>>>>>>>>>>",realtimeId);                                	
       	//????????????????????????????????????????????????
       	RealtimeFileDetectionResult reDetectionResult = realtimeFileDetectionNew(mobileSubList, customerId);  
       	if(reDetectionResult == null || reDetectionResult.getData() == null){
       		//?????????????????????????????????????????????????????????
       		log.error(">>>>>>>>>>>>>>> ??????????????????????????????????????????????????????????????????????????? realtimeId:{}>>>>>>>>>>>>>>>",realtimeId); 
       		realtimeFileService.saveMobileToTxtFile(fileUrl, mobileSubList, RealtimeTxtSuffixEnum.UNKNOWN);
       	}else{
           ListMultimap<RealtimeReportGroupEnum, String> group = reDetectionResult.getData();
           //????????????????????????
           realtimeFileService.saveTestResultData(customerId, fileUrl, realtimeId, group);
       	}                              	                                	
       }       
   }
    
    public RealtimeFileDetectionResult realtimeFileDetectionNew(List<String> mobileList,Long customerId) throws Exception {
    	RealtimeFileDetectionResult result = new RealtimeFileDetectionResult();
        ListMultimap<RealtimeReportGroupEnum, String> data = ArrayListMultimap.create();
 
        //??????api??????????????????
        List<MobileRealtimeStatus> mobileRealtimeStatusList = asyncInvoke(mobileList, customerId);
        Set<String> hasResultSet = new HashSet<>();
        if(!CollectionUtils.isEmpty(mobileRealtimeStatusList)){
        	for (MobileRealtimeStatus mobileRealtimeStatus : mobileRealtimeStatusList) {
        		if(mobileRealtimeStatus == null) {
        			continue;
        		}
        		
                String m = mobileRealtimeStatus.getMobile();
                hasResultSet.add(m);
                //1.????????????
                RealtimeFileDetectionEnumAndDelivrdSet fileDetectionEnumAndDelivrdSet = realtimeFileDetectionDelivrdNew(m,mobileRealtimeStatus.getStatus(),mobileRealtimeStatus.getMnpStatus());
                if (fileDetectionEnumAndDelivrdSet != null) {
                    if (fileDetectionEnumAndDelivrdSet.getDetectedStatus() == 0) {
                        //????????????
                    	RealtimeReportGroupEnum.FileDetection eFileDetectionEnum = fileDetectionEnumAndDelivrdSet.getFd();
                        //??????
                        if (eFileDetectionEnum != null) {
                            data.put(eFileDetectionEnum, m);
                            continue;
                        }
                    }
                }
                
                //2.??????????????????????????????????????????????????????
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
    	// ???100???????????????????????????????????????
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
    				log.error("{}, ???????????????????????????????????????info:",customerId,e);
    				return null;
    			}
    		}).collect(Collectors.toSet());
    		
    		result.addAll(resultSet);
    	}
    	
		return result;
    }
    
	private RealtimeFileDetectionEnumAndDelivrdSet realtimeFileDetectionDelivrdNew(String mobile,String status,String mnpStatus) {
		RealtimeFileDetectionEnumAndDelivrdSet result = new RealtimeFileDetectionEnumAndDelivrdSet();
		//????????????  0??????????????????   1???????????????   2???????????????
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
				//????????????????????????
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

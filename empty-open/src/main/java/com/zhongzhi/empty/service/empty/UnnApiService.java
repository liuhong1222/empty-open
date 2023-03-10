package com.zhongzhi.empty.service.empty;

import java.io.FileInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.entity.CustomerConsume;
import com.zhongzhi.empty.entity.CvsFilePath;
import com.zhongzhi.empty.entity.EmptyCheck;
import com.zhongzhi.empty.entity.MobileStatusCache;
import com.zhongzhi.empty.entity.PhoneSection;
import com.zhongzhi.empty.entity.SysConfig;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.enums.ProductEnum;
import com.zhongzhi.empty.enums.UserCheckTypeEnum;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.BatchCheckResult;
import com.zhongzhi.empty.response.FileCheckResult;
import com.zhongzhi.empty.response.LuaByDeductFeeResponse;
import com.zhongzhi.empty.response.UnnMobileNewStatus;
import com.zhongzhi.empty.response.UnnMobileStatus;
import com.zhongzhi.empty.service.LocalCacheService;
import com.zhongzhi.empty.service.PhoneSectionService;
import com.zhongzhi.empty.service.SysConfigService;
import com.zhongzhi.empty.service.balance.BalanceService;
import com.zhongzhi.empty.service.file.FileUploadService;
import com.zhongzhi.empty.service.gateway.UnnService;
import com.zhongzhi.empty.task.CustomerConsumeLocalCache;
import com.zhongzhi.empty.task.EmptyCheckLocalCache;
import com.zhongzhi.empty.task.MobilePoolLocalCache;
import com.zhongzhi.empty.util.DateUtils;
import com.zhongzhi.empty.util.FileUtil;
import com.zhongzhi.empty.util.Snowflake;
import com.zhongzhi.empty.util.ThreadLocalContainer;
import com.zhongzhi.empty.util.UploadUtil;
import com.zhongzhi.empty.vo.CustomerInfoVo;
import com.zhongzhi.empty.vo.UnnResultVo;

import lombok.extern.slf4j.Slf4j;

/**
 * ?????????????????????????????????
 * @author liuh
 * @date 2021???10???27???
 */
@Slf4j
@Service
public class UnnApiService {
	
	@Autowired
	private BalanceService balanceService;
	
	@Autowired
	private SysConfigService sysConfigService;
	
	@Autowired
	private UnnService unnService;
	
	@Autowired
	private LocalCacheService localCacheService;
	
	@Autowired
	private Snowflake snowflake;
	
	@Autowired
	private EmptyCheckService emptyCheckService;
	
	@Autowired
	private FileEmptyCheckService fileEmptyCheckService;
	
	@Value("${empty.file.upload.path}")
	private String fileUploadPath;
	
	@Value("${empty.file.resource.path}")
	private String fileResourcePath;
	
	@Autowired
	private PhoneSectionService phoneSectionService;
	
	@Autowired
	private FileUploadService fileUploadService;
	
	@Autowired
	private CvsFilePathService cvsFilePathService;
	
	private static EmptyCheckLocalCache emptyCheckLocalCache = EmptyCheckLocalCache.getInStance();
	
	private static CustomerConsumeLocalCache customerConsumeLocalCache = CustomerConsumeLocalCache.getInStance();
	
	private static MobilePoolLocalCache mobilePoolLocalCache = MobilePoolLocalCache.getInStance();

	public ApiResult<BatchCheckResult> batchCheck(String[] mobiles,String ip) {
		long st = System.currentTimeMillis();
		// ?????????????????????????????????
		CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
		Long id = snowflake.nextId();
		try {
			// ?????????
			boolean preDeductFeeFlag = balanceService.preDeductFee(customerInfoVo.getCustomerId(), 
						mobiles.length, ProductEnum.EMPTY.getProductCode(),id);
			if(!preDeductFeeFlag) {
		         return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "????????????", null);
			}

	        // ?????????????????????????????????????????????
			String gateway = "";
			SysConfig sysConfig = sysConfigService.findOneByKey(CommonConstant.EMPTY_GATEWAY);
			gateway = (sysConfig == null || sysConfig.getStatus() == 0) ? CommonConstant.EMPTY_GATEWAY_ONLINE_DEFAULT : sysConfig.getParamValue();
	        
			List<UnnMobileStatus> list = new ArrayList<UnnMobileStatus>();
			// ??????????????????
			UnnResultVo unnResultVo = invokeApi(customerInfoVo.getCustomerId(), gateway, mobiles);
	        if(unnResultVo != null) {
	        	list = unnResultVo.getList();
	        }
	        
	        if(CollectionUtils.isEmpty(list)) {
	        	//???????????????
	        	balanceService.backDeductFee(customerInfoVo.getCustomerId(), mobiles.length, ProductEnum.EMPTY.getProductCode(),id);
	        	
	        	log.error("{}, ????????????????????????????????????????????????????????????????????????gateway:{},mobiles:{}",customerInfoVo.getCustomerId(), gateway, mobiles);
	            return ApiResult.result(ApiCode.FAIL, "???????????????????????????????????????", null);
	        }

	        // ??????
	        LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerInfoVo.getCustomerId(), 
	        														mobiles.length, list.size(), ProductEnum.EMPTY.getProductCode(),id);
	        
	        // ??????????????????
	        EmptyCheck empty = getEmptyCheckDataByApi(list, customerInfoVo, id,unnResultVo);
	        emptyCheckLocalCache.setLocalCache(empty);
	         
	        // ??????????????????
	        CustomerConsume consume = getCustomerConsumeData(customerInfoVo, empty, luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance(),
	        		CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue());
	        customerConsumeLocalCache.setLocalCache(consume);
	        
	        // ???????????????0????????? 1????????? 4???????????? 5???????????? 12: ????????????
	        // ??????????????????
	        BatchCheckResult batchCheckResult = new BatchCheckResult();
	        batchCheckResult.setChargeCount(list.size());
	        batchCheckResult.setMobiles(list);
	        
	        log.info("{}, ?????????????????????????????????count:{},ip:{},useTime:{}",customerInfoVo.getCustomerId(),mobiles.length,ip,(System.currentTimeMillis()-st));
	        return ApiResult.ok(batchCheckResult);
		} catch (Exception e) {
			log.error("{}, ?????????????????????????????????count:{},ip???{}???info:",customerInfoVo.getCustomerId(),mobiles.length,ip,e);
			return ApiResult.result(ApiCode.FAIL, "????????????", null);
		}
	}
	
	public ApiResult<List<UnnMobileNewStatus>> batchCheckNew(String[] mobiles,String ip){
		long st = System.currentTimeMillis();
		// ?????????????????????????????????
		CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
		Long id = snowflake.nextId();
		try {
			// ?????????
			boolean preDeductFeeFlag = balanceService.preDeductFee(customerInfoVo.getCustomerId(), 
						mobiles.length, ProductEnum.EMPTY.getProductCode(),id);
			if(!preDeductFeeFlag) {
		         return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "????????????", null);
			}

	        // ?????????????????????????????????????????????
			String gateway = "";
			SysConfig sysConfig = sysConfigService.findOneByKey(CommonConstant.EMPTY_GATEWAY);
			gateway = (sysConfig == null || sysConfig.getStatus() == 0) ? CommonConstant.EMPTY_GATEWAY_ONLINE_DEFAULT : sysConfig.getParamValue();
	        
			List<UnnMobileStatus> list = new ArrayList<UnnMobileStatus>();
			// ??????????????????
			UnnResultVo unnResultVo = invokeApi(customerInfoVo.getCustomerId(), gateway, mobiles);
	        if(unnResultVo != null) {
	        	list = unnResultVo.getList();
	        }
	        
	        if(CollectionUtils.isEmpty(list)) {
	        	//???????????????
	        	balanceService.backDeductFee(customerInfoVo.getCustomerId(), mobiles.length, ProductEnum.EMPTY.getProductCode(),id);
	        	
	        	log.error("{}, ????????????????????????????????????????????????????????????????????????gateway:{},mobiles:{}",customerInfoVo.getCustomerId(), gateway, mobiles);
	            return ApiResult.result(ApiCode.FAIL, "???????????????????????????????????????", null);
	        }

	        // ??????
	        LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerInfoVo.getCustomerId(), 
	        														mobiles.length, list.size(), ProductEnum.EMPTY.getProductCode(),id);
	        
	        // ??????????????????
	        EmptyCheck empty = getEmptyCheckDataByApi(list, customerInfoVo, id,unnResultVo);
	        emptyCheckLocalCache.setLocalCache(empty);
	         
	        // ??????????????????
	        CustomerConsume consume = getCustomerConsumeData(customerInfoVo, empty, luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance(),
	        		CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue());
	        customerConsumeLocalCache.setLocalCache(consume);
	        
	        // ???????????????0????????? 1????????? 4???????????? 5???????????? 12: ????????????
	        // ??????????????????
	        List<UnnMobileNewStatus> resultList = getResultList(list);
	        
	        log.info("{}, ?????????????????????????????????count:{},ip:{},useTime:{}",customerInfoVo.getCustomerId(),mobiles.length,ip,(System.currentTimeMillis()-st));
	        return ApiResult.ok(resultList);
		} catch (Exception e) {
			log.error("{}, ?????????????????????????????????count:{},ip???{}???info:",customerInfoVo.getCustomerId(),mobiles.length,ip,e);
			return ApiResult.result(ApiCode.FAIL, "????????????", null);
		}
	}
	
	public ApiResult<FileCheckResult> uploadCheck(MultipartFile multipartFile,String ip){
		try {
			// ?????????????????????????????????
			CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
			// ????????????
	        String originalFileName = multipartFile.getOriginalFilename();
	        String fileExtension = FilenameUtils.getExtension(originalFileName);
	        if (fileExtension == null || !"txt".equals(fileExtension.toLowerCase())) {
	            log.error("?????????????????????, customerId: {}, ???????????????{}", customerInfoVo.getCustomerId(), originalFileName);
	            return ApiResult.result(ApiCode.FAIL, "?????????????????????", null);
	        }
	        // ????????????
	        if (multipartFile.getSize() > 40 * 1024 * 1024) {
	            log.warn("????????????40M, customerId: {}", customerInfoVo.getCustomerId());
	            return ApiResult.result(ApiCode.FAIL, "????????????40M????????????", null);
	        }
	        
	        Long emptyId = snowflake.nextId();
	        String sourceFileName = emptyId+ "_" +CommonConstant.SOURCE_FILE_NAME;
	        String uploadPath = fileUploadPath + "temp/" + DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/";
	        String filePath = uploadPath + sourceFileName;
	        
	        // ????????????
	        String saveFileName = UploadUtil.upload(uploadPath, multipartFile, originalFilename -> sourceFileName);
	        if (StringUtils.isBlank(saveFileName)) {
	        	log.error("{}, ?????????????????????saveFileName:{}",customerInfoVo.getCustomerId(),saveFileName);
                return ApiResult.fail("??????????????????");
            }
	        
	        // ??????????????????????????????
	        String fileMd5 = DigestUtils.md5Hex(new FileInputStream(filePath));
// 			String md5String = redisClient.get(String.format(RedisKeyConstant.FILE_MD5_CACHE_KEY, customerInfoVo.getCustomerId(),fileMd5));
// 			if(StringUtils.isNotBlank(md5String)) {
// 				return ApiResult.fail(String.format("??????????????????[%s]?????????????????????????????????", md5String));
// 			}
	     			
	        // ??????????????????????????????
 			int fileRows = FileUtil.getFileLineNum(filePath);
	        fileUploadService.saveOne(emptyId, customerInfoVo.getCustomerId(), originalFileName, filePath,fileRows,fileMd5);
	        
	        // ??????????????????
	        EmptyCheck emptyCheck = getEmptyCheckDataByFile(filePath, customerInfoVo, emptyId, multipartFile.getSize(), originalFileName,fileRows);
	        int counts = emptyCheckService.saveOne(emptyCheck);
	        if(counts != 1) {
	        	log.error("{}, ????????????????????????????????????????????????????????????????????????info:{}",customerInfoVo.getCustomerId(),JSON.toJSONString(emptyCheck));
	        	return ApiResult.fail(ApiCode.BUSINESS_EXCEPTION);
	        }
	        
	        // ??????????????????
	        CustomerConsume consume = getCustomerConsumeData(customerInfoVo, emptyCheck, 0L,CustomerConsume.ConsumeType.FREEZE.getValue());
	        customerConsumeLocalCache.setLocalCache(consume);
	        
	        //????????????
	        ApiResult apiResult = fileEmptyCheckService.executeEmptyCheck(emptyCheck.getCustomerId(), emptyCheck.getId(),
	        		                                  emptyCheck.getTotalNumber(),emptyCheck.getName(),uploadPath+saveFileName);
	        if(!apiResult.isSuccess()) {
	        	log.error("{}, api???????????????????????????filename:{},info:{}",emptyCheck.getCustomerId(),originalFileName,JSON.toJSONString(apiResult));
	        	return apiResult;
	        }
	        
	        FileCheckResult fileCheckResult = new FileCheckResult();
	        fileCheckResult.setStatus(EmptyCheck.EmptyCheckStatus.TO_DEDUCT.getStatus());
	        fileCheckResult.setSendId(emptyId);
	        fileCheckResult.setZip_url(fileResourcePath + DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + emptyId + "/" + emptyCheck.getName() + ".zip");
	        fileCheckResult.setActive_url(fileResourcePath + DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + emptyId + "/" + CommonConstant.ACTIVE_FILE_NAME);
	        fileCheckResult.setEmpty_url(fileResourcePath +  DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + emptyId + "/" + CommonConstant.EMPTY_FILE_NAME);
	        fileCheckResult.setRisk_url(fileResourcePath +  DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + emptyId + "/" + CommonConstant.RISK_FILE_NAME);
	        fileCheckResult.setSilent_url(fileResourcePath +  DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + emptyId + "/" + CommonConstant.SILENT_FILE_NAME);
	        
	        log.info("{}, ??????????????????api???????????????fileName:{},ip:{},emptyId:{}",customerInfoVo.getCustomerId(),multipartFile.getOriginalFilename(),ip,emptyId);
	        return ApiResult.ok(fileCheckResult);
		} catch (Exception e) {
			log.error("{}, ??????????????????api?????????file:{},info:",ip,JSON.toJSONString(multipartFile),e);
			return ApiResult.fail(ApiCode.SYSTEM_EXCEPTION);
		}
	}
	
	public ApiResult<FileCheckResult> uploadQuery(Long sendId,String ip){
		long st = System.currentTimeMillis();
		try {
			FileCheckResult fileCheckResult = new FileCheckResult();
			// ?????????????????????????????????
			CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
			// ????????????????????????
	        EmptyCheck emptyCheck = emptyCheckService.findOne(customerInfoVo.getCustomerId(), sendId);
	        if(emptyCheck == null) {
	        	return ApiResult.fail(ApiCode.BUSINESS_EXCEPTION,"????????????????????????");
	        }
	        
	        // ????????????
	        if (EmptyCheck.EmptyCheckStatus.WORK_FINISH.getStatus() == emptyCheck.getStatus()) {
	        	CvsFilePath cvsFilePath = cvsFilePathService.findOne(customerInfoVo.getCustomerId(), sendId);
	        	fileCheckResult.setStatus(emptyCheck.getStatus());
		        fileCheckResult.setSendId(sendId);
		        if(StringUtils.isNotBlank(cvsFilePath.getZipPath())) {
		        	fileCheckResult.setZip_url(fileResourcePath +  cvsFilePath.getZipPath());
		        }
		        
		        if(StringUtils.isNotBlank(cvsFilePath.getRealFilePath())) {
		        	fileCheckResult.setActive_url(fileResourcePath +  cvsFilePath.getRealFilePath());
		        }
		        
				if(StringUtils.isNotBlank(cvsFilePath.getEmptyFilePath())) {
					fileCheckResult.setEmpty_url(fileResourcePath +  cvsFilePath.getEmptyFilePath());	        	
				}
				
				if(StringUtils.isNotBlank(cvsFilePath.getRiskFilePath())) {
					fileCheckResult.setRisk_url(fileResourcePath +  cvsFilePath.getRiskFilePath());
				}
				
				if(StringUtils.isNotBlank(cvsFilePath.getSilentFilePath())) {
					fileCheckResult.setSilent_url(fileResourcePath +  cvsFilePath.getSilentFilePath());
				}
	        }else {
	        	fileCheckResult.setStatus(emptyCheck.getStatus());
		        fileCheckResult.setSendId(sendId);
		        fileCheckResult.setZip_url(fileResourcePath +  DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + sendId + "/" + emptyCheck.getName() + ".zip");
		        fileCheckResult.setActive_url(fileResourcePath +  DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + sendId + "/" + CommonConstant.ACTIVE_FILE_NAME);
		        fileCheckResult.setEmpty_url(fileResourcePath +  DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + sendId + "/" + CommonConstant.EMPTY_FILE_NAME);
		        fileCheckResult.setRisk_url(fileResourcePath +  DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + sendId + "/" + CommonConstant.RISK_FILE_NAME);
		        fileCheckResult.setSilent_url(fileResourcePath +  DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/" + sendId + "/" + CommonConstant.SILENT_FILE_NAME);
	        }
			
	        log.info("{}, ????????????????????????api???????????????emptyId:{},ip:{},useTime:{}",customerInfoVo.getCustomerId(),sendId,ip,(System.currentTimeMillis()-st));
	        return ApiResult.ok(fileCheckResult);
		} catch (Exception e) {
			log.error("????????????????????????api???????????????emptyId:{},info:",sendId,e);
			return ApiResult.fail(ApiCode.SYSTEM_EXCEPTION);
		}
	}
	
	/**
	 * ????????????
	 * @param customerId
	 * @param gateway
	 * @param mobiles
	 * @return
	 */
	public UnnResultVo invokeApi(Long customerId,String gateway,String[] mobiles){
		try {
			UnnResultVo unnResultVo = new UnnResultVo();
			//????????????????????????
			String[] gatewayList = gateway.split(",");
			if(gatewayList.length > 1) {
				//??????????????????
				unnResultVo = invokeApiByMulitGateway(customerId, gatewayList, mobiles);
			}else {
				//??????????????????
				unnResultVo = invokeApiBySingleGateway(customerId, gateway, mobiles);
			}
			
			//?????????????????????????????????
			return replenishMobileStatus(mobiles, unnResultVo);
		} catch (Exception e) {
			log.error("{}????????????????????????info:",customerId,e);
			return null;
		}
		
	}
	
	/**
	 * ??????????????????
	 * @param customerId
	 * @param gateway
	 * @param mobiles
	 * @return
	 */
	private UnnResultVo invokeApiBySingleGateway(Long customerId,String gateway,String[] mobiles){
		List<UnnMobileStatus> list = new ArrayList<UnnMobileStatus>();
		int noCacheCount = 0;
		if(CommonConstant.CHUANGLAN_EMPTY_GATEWAY.equals(gateway)) {
			list = unnService.emptyCheck(customerId, mobiles);
			noCacheCount = list.size();
			// ???????????????
			if(!CollectionUtils.isEmpty(list)) {
				// ?????????????????????
		        List<MobileStatusCache> mobileStatusCacheList = getMobileStatusCacheData(list);
		        mobilePoolLocalCache.setLocalCache(mobileStatusCacheList);
			}
		}else if(CommonConstant.LOCAL_EMPTY_GATEWAY.equals(gateway)) {
			list = localCacheService.emptyCheck(customerId, mobiles);
		}else {
			log.error("{}, ?????????????????????????????????????????????, gateway:{}",customerId,gateway);
		}
		
		return new UnnResultVo(list, noCacheCount);		
	}
	
	/**
	 * ???????????????
	 * @param customerId
	 * @param gateway
	 * @param mobiles
	 * @return
	 */
	public UnnResultVo invokeApiByMulitGateway(Long customerId,String[] gatewayList,String[] mobiles){
		//????????????????????????????????????????????????????????????
		if(!CommonConstant.LOCAL_EMPTY_GATEWAY.equals(gatewayList[0])) {
			return invokeApiBySingleGateway(customerId, CommonConstant.CHUANGLAN_EMPTY_GATEWAY, mobiles);
		}
		
		List<String> mobileList = new ArrayList<String>(Arrays.asList(mobiles));
		//?????????????????????
		UnnResultVo tempCacheResult = invokeApiBySingleGateway(customerId, gatewayList[0], mobiles);
		List<UnnMobileStatus> list = tempCacheResult.getList();
		if(!CollectionUtils.isEmpty(list)) {
			List<String> resultMobileList = list.stream().map(um -> {return um.getMobile();}).collect(Collectors.toList());
			mobileList.removeAll(resultMobileList);
		}
		
		if(CollectionUtils.isEmpty(mobileList)) {
			return tempCacheResult;
		}
		
		//??????????????????
		UnnResultVo tempResult = invokeApiBySingleGateway(customerId, gatewayList[1], mobileList.toArray(new String[mobileList.size()]));
		List<UnnMobileStatus> list2 = tempResult.getList();
		if(CollectionUtils.isEmpty(list)) {
			return tempResult;
		}else {
			list.addAll(list2);
			return new UnnResultVo(list, tempResult.getNoCacheCount());
		}
	}
	
	/**
	 * ?????????????????????????????????????????????????????????
	 * @param mobiles
	 * @param list
	 * @return
	 */
	private UnnResultVo replenishMobileStatus(String[] mobiles,UnnResultVo unnResultVo) {
		int count = ((unnResultVo==null || CollectionUtils.isEmpty(unnResultVo.getList()))?0:unnResultVo.getList().size());
		if(mobiles.length < count) {
			return null;
		}else if(mobiles.length == count) {
			return unnResultVo;
		}else {
			int noCacheCount = 0;
			List<String> mobileList = new ArrayList<String>(Arrays.asList(mobiles));
			List<UnnMobileStatus> list = new ArrayList<UnnMobileStatus>();
			if(unnResultVo != null && !CollectionUtils.isEmpty(unnResultVo.getList())) {
				List<String> resultMobileList = unnResultVo.getList().stream().map(um -> {return um.getMobile();}).collect(Collectors.toList());
				mobileList.removeAll(resultMobileList);
				list.addAll(unnResultVo.getList());
				noCacheCount = unnResultVo.getNoCacheCount();
			}
			
			for(String mobile:mobileList) {
				UnnMobileStatus unnMobileStatus = new UnnMobileStatus();
				unnMobileStatus.setMobile(mobile);
				unnMobileStatus.setChargesStatus(CommonConstant.MOBILE_CHARGE_STATUS);
				unnMobileStatus.setStatus(CommonConstant.SLIENCE_MOBILE_STATUS);
				list.add(unnMobileStatus);
			}
			
			return new UnnResultVo(list,noCacheCount);
		}
	}
	
	private EmptyCheck getEmptyCheckDataByApi(List<UnnMobileStatus> list,CustomerInfoVo customerInfoVo,Long id,UnnResultVo unnResultVo) {
		//??????????????????
		Map<String, List<UnnMobileStatus>> map = list.stream().collect(Collectors.groupingBy(UnnMobileStatus::getStatus));
		
		EmptyCheck emptyCheck = new EmptyCheck();
		emptyCheck.setStatus(EmptyCheck.EmptyCheckStatus.WORK_FINISH.getStatus())
        .setFileUrl("")
        .setRealNumber(Long.valueOf(map.get("1")==null?0:map.get("1").size()))
        .setRiskNumber(Long.valueOf(map.get("5")==null?0:map.get("5").size()))
        .setSilentNumber(Long.valueOf(map.get("4")==null?0:map.get("4").size()))
        .setEmptyNumber(Long.valueOf(map.get("0")==null?0:map.get("0").size()))
        .setIllegalNumber(0L)
        .setUnknownNumber(unnResultVo==null?0L:Long.valueOf(unnResultVo.getNoCacheCount()))
        .setTotalNumber(Long.valueOf(list.size()))
        .setSize("0")
        .setAgentId(customerInfoVo.getAgentId())
        .setAgentName(customerInfoVo.getCompanyName())
        .setCustomerId(customerInfoVo.getCustomerId())
        .setName("mobiles_" + list.get(0).getMobile())
        .setSendId("200")
        .setId(id)
        .setCacheFinish(1)
        .setRetryCount(0)
        .setDeleted(0)
        .setVersion(0)
        .setMd5(null)
        .setRemark(UserCheckTypeEnum.API.getName())
        .setCategory(ProductEnum.EMPTY.getProductCode());
		return emptyCheck;
	}
	
	private EmptyCheck getEmptyCheckDataByFile(String filePath,CustomerInfoVo customerInfoVo,Long id,Long fileSize,String originalFileName,int fileRows) {		
		EmptyCheck emptyCheck = new EmptyCheck();
		emptyCheck.setStatus(EmptyCheck.EmptyCheckStatus.TO_DEDUCT.getStatus())
        .setFileUrl(filePath)
        .setRealNumber(null)
        .setRiskNumber(null)
        .setSilentNumber(null)
        .setEmptyNumber(null)
        .setIllegalNumber(null)
        .setTotalNumber(Long.valueOf(fileRows))
        .setSize(fileSize.toString())
        .setAgentId(customerInfoVo.getAgentId())
        .setAgentName(customerInfoVo.getCompanyName())
        .setCustomerId(customerInfoVo.getCustomerId())
        .setName(originalFileName.substring(0, originalFileName.lastIndexOf(".")))
        .setSendId(null)
        .setId(id)
        .setCacheFinish(1)
        .setRetryCount(0)
        .setDeleted(0)
        .setVersion(0)
        .setMd5(null)
        .setSendCount(Long.valueOf(fileRows))
        .setRemark(UserCheckTypeEnum.API_UPLOAD.getName())
        .setCategory(ProductEnum.EMPTY.getProductCode());
		return emptyCheck;
	}
	
	private CustomerConsume getCustomerConsumeData(CustomerInfoVo customerInfoVo,EmptyCheck emptyCheck,Long balance,Integer consumeType) {
		CustomerConsume customerConsume = new CustomerConsume();
		customerConsume.setAgentId(customerInfoVo.getAgentId())
		.setId(snowflake.nextId())
        .setConsumeNumber(emptyCheck.getTotalNumber())
        .setCustomerId(customerInfoVo.getCustomerId())
        .setName(customerInfoVo.getCustomerName())
        .setPhone(customerInfoVo.getPhone())
        .setVersion(0)
        .setCategory(ProductEnum.EMPTY.getProductCode())
        .setConsumeType(consumeType)
        .setEmptyId(emptyCheck.getId())
        .setOpeningBalance(balance + customerConsume.getConsumeNumber())
        .setClosingBalance(balance);
		return customerConsume;
	}
	
	private List<MobileStatusCache> getMobileStatusCacheData(List<UnnMobileStatus> tempList) {
		List<MobileStatusCache> list = new ArrayList<MobileStatusCache>();
		for(UnnMobileStatus unnMobileStatus : tempList) {
			MobileStatusCache mobileStatusCache = new MobileStatusCache();
			mobileStatusCache.setMobile(unnMobileStatus.getMobile());
			mobileStatusCache.setStatus(unnMobileStatus.getStatus());
			list.add(mobileStatusCache);
		}
		
		return list;
	}
	
	private List<UnnMobileNewStatus> getResultList(List<UnnMobileStatus> list) throws ParseException{
		List<UnnMobileNewStatus> resultList = new ArrayList<UnnMobileNewStatus>();
		for(UnnMobileStatus unnMobileStatus : list) {
			UnnMobileNewStatus unnMobileNewStatus = new UnnMobileNewStatus();
			unnMobileNewStatus.setChargesStatus(unnMobileStatus.getChargesStatus());
			unnMobileNewStatus.setMobile(unnMobileStatus.getMobile());
			unnMobileNewStatus.setStatus(unnMobileStatus.getStatus());
			unnMobileNewStatus.setLastTime(String.valueOf(DateUtils.randomDate(DateUtils.addMonth(DateUtils.getCurrentDateTime(), -1), DateUtils.getCurrentDateTime()).getTime()));
			
			PhoneSection phoneSection = phoneSectionService.getPhoneSection(unnMobileStatus.getMobile().substring(0, 7));
			if(phoneSection != null) {
				unnMobileNewStatus.setArea(phoneSection.getProvince() + "-" + phoneSection.getCity());
				unnMobileNewStatus.setNumberType("??????" + phoneSection.getIsp());
			}
			
			resultList.add(unnMobileNewStatus);
		}
		
		return resultList;
	}
}

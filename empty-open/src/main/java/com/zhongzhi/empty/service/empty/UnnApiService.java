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
 * 空号检测系列接口实现类
 * @author liuh
 * @date 2021年10月27日
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
		// 线程缓存里获取用户信息
		CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
		Long id = snowflake.nextId();
		try {
			// 预扣费
			boolean preDeductFeeFlag = balanceService.preDeductFee(customerInfoVo.getCustomerId(), 
						mobiles.length, ProductEnum.EMPTY.getProductCode(),id);
			if(!preDeductFeeFlag) {
		         return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足", null);
			}

	        // 获取系统当前设置的空号检测通道
			String gateway = "";
			SysConfig sysConfig = sysConfigService.findOneByKey(CommonConstant.EMPTY_GATEWAY);
			gateway = (sysConfig == null || sysConfig.getStatus() == 0) ? CommonConstant.EMPTY_GATEWAY_ONLINE_DEFAULT : sysConfig.getParamValue();
	        
			List<UnnMobileStatus> list = new ArrayList<UnnMobileStatus>();
			// 执行空号检测
			UnnResultVo unnResultVo = invokeApi(customerInfoVo.getCustomerId(), gateway, mobiles);
	        if(unnResultVo != null) {
	        	list = unnResultVo.getList();
	        }
	        
	        if(CollectionUtils.isEmpty(list)) {
	        	//返还预扣费
	        	balanceService.backDeductFee(customerInfoVo.getCustomerId(), mobiles.length, ProductEnum.EMPTY.getProductCode(),id);
	        	
	        	log.error("{}, 空号检测失败，系统异常或结果条数大于提交号码数，gateway:{},mobiles:{}",customerInfoVo.getCustomerId(), gateway, mobiles);
	            return ApiResult.result(ApiCode.FAIL, "系统异常，空号检测结果为空", null);
	        }

	        // 扣费
	        LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerInfoVo.getCustomerId(), 
	        														mobiles.length, list.size(), ProductEnum.EMPTY.getProductCode(),id);
	        
	        // 检测记录入库
	        EmptyCheck empty = getEmptyCheckDataByApi(list, customerInfoVo, id,unnResultVo);
	        emptyCheckLocalCache.setLocalCache(empty);
	         
	        // 消费记录入库
	        CustomerConsume consume = getCustomerConsumeData(customerInfoVo, empty, luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance(),
	        		CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue());
	        customerConsumeLocalCache.setLocalCache(consume);
	        
	        // 检测结果，0：空号 1：实号 4：沉默号 5：风险号 12: 号码错误
	        // 封装返回数据
	        BatchCheckResult batchCheckResult = new BatchCheckResult();
	        batchCheckResult.setChargeCount(list.size());
	        batchCheckResult.setMobiles(list);
	        
	        log.info("{}, 空号检测接口调用成功，count:{},ip:{},useTime:{}",customerInfoVo.getCustomerId(),mobiles.length,ip,(System.currentTimeMillis()-st));
	        return ApiResult.ok(batchCheckResult);
		} catch (Exception e) {
			log.error("{}, 空号检测接口调用异常，count:{},ip：{}，info:",customerInfoVo.getCustomerId(),mobiles.length,ip,e);
			return ApiResult.result(ApiCode.FAIL, "系统异常", null);
		}
	}
	
	public ApiResult<List<UnnMobileNewStatus>> batchCheckNew(String[] mobiles,String ip){
		long st = System.currentTimeMillis();
		// 线程缓存里获取用户信息
		CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
		Long id = snowflake.nextId();
		try {
			// 预扣费
			boolean preDeductFeeFlag = balanceService.preDeductFee(customerInfoVo.getCustomerId(), 
						mobiles.length, ProductEnum.EMPTY.getProductCode(),id);
			if(!preDeductFeeFlag) {
		         return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足", null);
			}

	        // 获取系统当前设置的空号检测通道
			String gateway = "";
			SysConfig sysConfig = sysConfigService.findOneByKey(CommonConstant.EMPTY_GATEWAY);
			gateway = (sysConfig == null || sysConfig.getStatus() == 0) ? CommonConstant.EMPTY_GATEWAY_ONLINE_DEFAULT : sysConfig.getParamValue();
	        
			List<UnnMobileStatus> list = new ArrayList<UnnMobileStatus>();
			// 执行空号检测
			UnnResultVo unnResultVo = invokeApi(customerInfoVo.getCustomerId(), gateway, mobiles);
	        if(unnResultVo != null) {
	        	list = unnResultVo.getList();
	        }
	        
	        if(CollectionUtils.isEmpty(list)) {
	        	//返还预扣费
	        	balanceService.backDeductFee(customerInfoVo.getCustomerId(), mobiles.length, ProductEnum.EMPTY.getProductCode(),id);
	        	
	        	log.error("{}, 空号检测失败，系统异常或结果条数大于提交号码数，gateway:{},mobiles:{}",customerInfoVo.getCustomerId(), gateway, mobiles);
	            return ApiResult.result(ApiCode.FAIL, "系统异常，空号检测结果为空", null);
	        }

	        // 扣费
	        LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerInfoVo.getCustomerId(), 
	        														mobiles.length, list.size(), ProductEnum.EMPTY.getProductCode(),id);
	        
	        // 检测记录入库
	        EmptyCheck empty = getEmptyCheckDataByApi(list, customerInfoVo, id,unnResultVo);
	        emptyCheckLocalCache.setLocalCache(empty);
	         
	        // 消费记录入库
	        CustomerConsume consume = getCustomerConsumeData(customerInfoVo, empty, luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance(),
	        		CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue());
	        customerConsumeLocalCache.setLocalCache(consume);
	        
	        // 检测结果，0：空号 1：实号 4：沉默号 5：风险号 12: 号码错误
	        // 封装返回数据
	        List<UnnMobileNewStatus> resultList = getResultList(list);
	        
	        log.info("{}, 空号检测接口调用成功，count:{},ip:{},useTime:{}",customerInfoVo.getCustomerId(),mobiles.length,ip,(System.currentTimeMillis()-st));
	        return ApiResult.ok(resultList);
		} catch (Exception e) {
			log.error("{}, 空号检测接口调用异常，count:{},ip：{}，info:",customerInfoVo.getCustomerId(),mobiles.length,ip,e);
			return ApiResult.result(ApiCode.FAIL, "系统异常", null);
		}
	}
	
	public ApiResult<FileCheckResult> uploadCheck(MultipartFile multipartFile,String ip){
		try {
			// 线程缓存里获取用户信息
			CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
			// 文件后缀
	        String originalFileName = multipartFile.getOriginalFilename();
	        String fileExtension = FilenameUtils.getExtension(originalFileName);
	        if (fileExtension == null || !"txt".equals(fileExtension.toLowerCase())) {
	            log.error("文件类型不允许, customerId: {}, 文件名称：{}", customerInfoVo.getCustomerId(), originalFileName);
	            return ApiResult.result(ApiCode.FAIL, "文件类型不允许", null);
	        }
	        // 文件大小
	        if (multipartFile.getSize() > 40 * 1024 * 1024) {
	            log.warn("文件超过40M, customerId: {}", customerInfoVo.getCustomerId());
	            return ApiResult.result(ApiCode.FAIL, "文件超过40M无法上传", null);
	        }
	        
	        Long emptyId = snowflake.nextId();
	        String sourceFileName = emptyId+ "_" +CommonConstant.SOURCE_FILE_NAME;
	        String uploadPath = fileUploadPath + "temp/" + DateUtils.getDate() + "/" + customerInfoVo.getCustomerId() + "/";
	        String filePath = uploadPath + sourceFileName;
	        
	        // 上传文件
	        String saveFileName = UploadUtil.upload(uploadPath, multipartFile, originalFilename -> sourceFileName);
	        if (StringUtils.isBlank(saveFileName)) {
	        	log.error("{}, 上传文件失败，saveFileName:{}",customerInfoVo.getCustomerId(),saveFileName);
                return ApiResult.fail("文件上传失败");
            }
	        
	        // 判断文件是否已经检测
	        String fileMd5 = DigestUtils.md5Hex(new FileInputStream(filePath));
// 			String md5String = redisClient.get(String.format(RedisKeyConstant.FILE_MD5_CACHE_KEY, customerInfoVo.getCustomerId(),fileMd5));
// 			if(StringUtils.isNotBlank(md5String)) {
// 				return ApiResult.fail(String.format("该文件与文件[%s]内容一致，请勿重复检测", md5String));
// 			}
	     			
	        // 异步保存文件上传记录
 			int fileRows = FileUtil.getFileLineNum(filePath);
	        fileUploadService.saveOne(emptyId, customerInfoVo.getCustomerId(), originalFileName, filePath,fileRows,fileMd5);
	        
	        // 保存检测记录
	        EmptyCheck emptyCheck = getEmptyCheckDataByFile(filePath, customerInfoVo, emptyId, multipartFile.getSize(), originalFileName,fileRows);
	        int counts = emptyCheckService.saveOne(emptyCheck);
	        if(counts != 1) {
	        	log.error("{}, 上传文件检测失败，检测记录入库失败，数据库异常，info:{}",customerInfoVo.getCustomerId(),JSON.toJSONString(emptyCheck));
	        	return ApiResult.fail(ApiCode.BUSINESS_EXCEPTION);
	        }
	        
	        // 消费记录入库
	        CustomerConsume consume = getCustomerConsumeData(customerInfoVo, emptyCheck, 0L,CustomerConsume.ConsumeType.FREEZE.getValue());
	        customerConsumeLocalCache.setLocalCache(consume);
	        
	        //进行检测
	        ApiResult apiResult = fileEmptyCheckService.executeEmptyCheck(emptyCheck.getCustomerId(), emptyCheck.getId(),
	        		                                  emptyCheck.getTotalNumber(),emptyCheck.getName(),uploadPath+saveFileName);
	        if(!apiResult.isSuccess()) {
	        	log.error("{}, api提交文件检测失败，filename:{},info:{}",emptyCheck.getCustomerId(),originalFileName,JSON.toJSONString(apiResult));
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
	        
	        log.info("{}, 空号在线检测api调用成功，fileName:{},ip:{},emptyId:{}",customerInfoVo.getCustomerId(),multipartFile.getOriginalFilename(),ip,emptyId);
	        return ApiResult.ok(fileCheckResult);
		} catch (Exception e) {
			log.error("{}, 空号在线检测api异常，file:{},info:",ip,JSON.toJSONString(multipartFile),e);
			return ApiResult.fail(ApiCode.SYSTEM_EXCEPTION);
		}
	}
	
	public ApiResult<FileCheckResult> uploadQuery(Long sendId,String ip){
		long st = System.currentTimeMillis();
		try {
			FileCheckResult fileCheckResult = new FileCheckResult();
			// 线程缓存里获取用户信息
			CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
			// 文件是否检测完成
	        EmptyCheck emptyCheck = emptyCheckService.findOne(customerInfoVo.getCustomerId(), sendId);
	        if(emptyCheck == null) {
	        	return ApiResult.fail(ApiCode.BUSINESS_EXCEPTION,"不存在的检测记录");
	        }
	        
	        // 检测完成
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
			
	        log.info("{}, 查询空号在线检测api结果成功，emptyId:{},ip:{},useTime:{}",customerInfoVo.getCustomerId(),sendId,ip,(System.currentTimeMillis()-st));
	        return ApiResult.ok(fileCheckResult);
		} catch (Exception e) {
			log.error("查询空号在线检测api结果异常，emptyId:{},info:",sendId,e);
			return ApiResult.fail(ApiCode.SYSTEM_EXCEPTION);
		}
	}
	
	/**
	 * 通道调用
	 * @param customerId
	 * @param gateway
	 * @param mobiles
	 * @return
	 */
	public UnnResultVo invokeApi(Long customerId,String gateway,String[] mobiles){
		try {
			UnnResultVo unnResultVo = new UnnResultVo();
			//区分是否为多通道
			String[] gatewayList = gateway.split(",");
			if(gatewayList.length > 1) {
				//混合通道检测
				unnResultVo = invokeApiByMulitGateway(customerId, gatewayList, mobiles);
			}else {
				//单条通道检测
				unnResultVo = invokeApiBySingleGateway(customerId, gateway, mobiles);
			}
			
			//补充查无结果的号码状态
			return replenishMobileStatus(mobiles, unnResultVo);
		} catch (Exception e) {
			log.error("{}，空号检测异常，info:",customerId,e);
			return null;
		}
		
	}
	
	/**
	 * 单个通道调用
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
			// 存入缓存库
			if(!CollectionUtils.isEmpty(list)) {
				// 号码池缓存入库
		        List<MobileStatusCache> mobileStatusCacheList = getMobileStatusCacheData(list);
		        mobilePoolLocalCache.setLocalCache(mobileStatusCacheList);
			}
		}else if(CommonConstant.LOCAL_EMPTY_GATEWAY.equals(gateway)) {
			list = localCacheService.emptyCheck(customerId, mobiles);
		}else {
			log.error("{}, 空号检测失败，配置的通道不存在, gateway:{}",customerId,gateway);
		}
		
		return new UnnResultVo(list, noCacheCount);		
	}
	
	/**
	 * 多通道混用
	 * @param customerId
	 * @param gateway
	 * @param mobiles
	 * @return
	 */
	public UnnResultVo invokeApiByMulitGateway(Long customerId,String[] gatewayList,String[] mobiles){
		//主通道不为本地缓存时，直接走创蓝的单通道
		if(!CommonConstant.LOCAL_EMPTY_GATEWAY.equals(gatewayList[0])) {
			return invokeApiBySingleGateway(customerId, CommonConstant.CHUANGLAN_EMPTY_GATEWAY, mobiles);
		}
		
		List<String> mobileList = new ArrayList<String>(Arrays.asList(mobiles));
		//查询缓存号码池
		UnnResultVo tempCacheResult = invokeApiBySingleGateway(customerId, gatewayList[0], mobiles);
		List<UnnMobileStatus> list = tempCacheResult.getList();
		if(!CollectionUtils.isEmpty(list)) {
			List<String> resultMobileList = list.stream().map(um -> {return um.getMobile();}).collect(Collectors.toList());
			mobileList.removeAll(resultMobileList);
		}
		
		if(CollectionUtils.isEmpty(mobileList)) {
			return tempCacheResult;
		}
		
		//调用实际通道
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
	 * 补充无查询结果的号码状态，默认为沉默号
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
		//按照状态分组
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
				unnMobileNewStatus.setNumberType("中国" + phoneSection.getIsp());
			}
			
			resultList.add(unnMobileNewStatus);
		}
		
		return resultList;
	}
}

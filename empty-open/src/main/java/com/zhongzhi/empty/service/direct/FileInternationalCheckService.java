package com.zhongzhi.empty.service.direct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.constants.IntDirectRedisKeyConstant;
import com.zhongzhi.empty.constants.InternationalRedisKeyConstant;
import com.zhongzhi.empty.entity.Customer;
import com.zhongzhi.empty.entity.CustomerConsume;
import com.zhongzhi.empty.entity.IntDirectCheck;
import com.zhongzhi.empty.entity.IntDirectCvsFilePath;
import com.zhongzhi.empty.entity.InternationalCheck;
import com.zhongzhi.empty.entity.InternationalCvsFilePath;
import com.zhongzhi.empty.entity.InternationalRunTestDomian;
import com.zhongzhi.empty.entity.TxtFileContent;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.enums.DirectTypeEnum;
import com.zhongzhi.empty.enums.IntDirectTxtSuffixEnum;
import com.zhongzhi.empty.enums.InternationalTxtSuffixEnum;
import com.zhongzhi.empty.enums.ProductEnum;
import com.zhongzhi.empty.http.international.QueryResponse;
import com.zhongzhi.empty.http.international.UploadResponse;
import com.zhongzhi.empty.redis.DistributedLockWrapper;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.service.balance.BalanceService;
import com.zhongzhi.empty.service.customer.CustomerConsumeService;
import com.zhongzhi.empty.service.customer.CustomerService;
import com.zhongzhi.empty.service.file.FileRedisService;
import com.zhongzhi.empty.service.file.FileUploadService;
import com.zhongzhi.empty.service.gateway.InternationalService;
import com.zhongzhi.empty.service.international.InternationalCheckService;
import com.zhongzhi.empty.service.international.InternationalCvsFilePathService;
import com.zhongzhi.empty.service.international.InternationalFileService;
import com.zhongzhi.empty.util.DingDingMessage;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPool;

/**
 * 定向国际检测实现类
 * @author liuh
 * @date 2022年10月18日
 */
@Slf4j
@Service
public class FileInternationalCheckService {

	@Autowired
	private JedisPool jedisPool;

	@Autowired
	private FileRedisService fileRedisService;

	@Autowired
	private IntDirectFileService intDirectFileService;

	@Autowired
	private RedisClient redisClient;

	@Autowired
	private BalanceService balanceService;

	@Autowired
	private DingDingMessage dingDingMessage;

	@Autowired
	private InternationalService internationalService;
	
	@Autowired
	private InternationalCheckService internationalCheckService;
	
	@Autowired
	private InternationalCvsFilePathService internationalCvsFilePathService;

	@Autowired
	private CustomerConsumeService customerConsumeService;

	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private IntDirectCheckService intDirectCheckService;
	
	@Autowired
	private IntDirectCvsFilePathService intDirectCvsFilePathService;
	
	@Autowired
	private FileUploadService fileUploadService;
	
	@Autowired
	private DirectCheckProgressService directCheckProgressService;
	
	@Autowired
	private InternationalFileService fileService;
	
	public ApiResult executeInternationalCheck(Long customerId, Long internationalId, Long totalNumber,
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

		try {
			log.info("----------用户[{}]开始执行国际检测检索事件,internationalId：{}", customerId, internationalId);
			// 2.获取有效的手机号码以及文件编码格式
			TxtFileContent fileContent = fileService.getValidMobileListByInternationalTxt(uploadPath,countryCode);
			if (fileContent == null || fileContent.getMobileCounts() == null || fileContent.getMobileCounts() < 2000
					|| fileContent.getMobileCounts() > 2000000) {
				lock.releaseLock();
				redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId),
						CommonConstant.FILE_TEST_FAILED_CODE, expire);
				return ApiResult.result(ApiCode.COUNT_EXCEPTION, "有效号码数不足2000或大于2000000【" + fileContent.getMobileCounts() + "】", null);
			}

			mobileCount = fileContent.getMobileCounts();
			// 3.预扣账户余额
			Boolean isFreeze = balanceService.preDeductFee(customerId, mobileCount,
					ProductEnum.INTERNATIONAL.getProductCode(), internationalId);
			if (!isFreeze) {
				lock.releaseLock();
				redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId),
						CommonConstant.FILE_TEST_FAILED_CODE, expire);
				return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足或预扣失败", null);
			}
						
			// 4.调用聚赢上传接口
			String uploadFilePath = fileService.getTxtPath(uploadPath, InternationalTxtSuffixEnum.ALL);
			UploadResponse uploadResponse = internationalService.upload(customerId, 
					uploadFilePath.substring(uploadFilePath.lastIndexOf("/")).replace("/", "").replace(".txt", ""),
					uploadFilePath);
			if (uploadResponse == null) {
				lock.releaseLock();
				// 返还预扣费
				balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),
						internationalId);
				redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId, internationalId),
						CommonConstant.FILE_TEST_FAILED_CODE, expire);
				return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "上游接口调用异常", null);
			}
			
			// 5.提交的有效号码总个数存入redis
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
			
			sendID = uploadResponse.getSendID();
			// 6.初始化国际检测相关配置redis值
			fileRedisService.internationalRedisInit(customerId, internationalId, expire, lock.getIdentifier(),
					Long.valueOf(mobileCount));
			// 7.保存临时文件
			fileService.saveTempFileByAll(uploadPath, customerId, fileContent.getMobileList(), internationalId);
			
			// 8.执行国际检测进度查询定时任务
			directCheckProgressService.timingExecute(getTaskInfo(countryCode, customerId, sendID, uploadPath, internationalId, null, sourceFileName,mobileCount));
			
			log.info("{}, 国际检测文件提交到下游成功，countryCode：{}，productType：{}，sendID：{}, internationalId:{}",customerId,countryCode,productType,sendID,internationalId);
		} catch (Exception e) {
			lock.releaseLock();
			// 返还预扣费
			balanceService.backDeductFee(customerId, mobileCount, ProductEnum.INTERNATIONAL.getProductCode(),
					internationalId);

			log.error("{}, 国际在线检测异常，internationalId:{},info:", customerId, internationalId, e);
			dingDingMessage.sendMessage(String.format("警告： 用户【%s】国际在线检测生成临时文件异常，internationalId:%s，info:%s", customerId,
					internationalId, e));
			return ApiResult.fail("国际检测异常");
		}

		lock.releaseLock();
		return ApiResult.ok(new InternationalRunTestDomian(mobileCount, CommonConstant.THETEST_RUNNING,
				internationalId.toString(), sendID));
	}
	
	public ApiResult executeIntDirectCheck(Long customerId, Long intDirectId, Long totalNumber,
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
				String.format(IntDirectRedisKeyConstant.THE_TEST_FUN_KEY, customerId, intDirectId),
				1000L * 60 * 60, expire);
		if (StringUtils.isBlank(lock.getIdentifier())) {
			log.error("{}, 该文件已提交检测，intDirectId:{}", customerId, intDirectId);
			return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "已提交检测，请勿重复提交", null);
		}

		try {
			log.info("----------用户[{}]开始执行定向国际检测检索事件,intDirectId：{}", customerId, intDirectId);
			// 2.获取有效的手机号码以及文件编码格式
			TxtFileContent fileContent = intDirectFileService.getValidMobileListByIntDirectTxt(uploadPath,
					countryCode);
			if (fileContent == null || fileContent.getMobileCounts() == null || fileContent.getMobileCounts() < 2000
					|| fileContent.getMobileCounts() > 2000000) {
				lock.releaseLock();
				redisClient.set(String.format(IntDirectRedisKeyConstant.EXCEPTION_KEY, customerId, intDirectId),
						CommonConstant.FILE_TEST_FAILED_CODE, expire);
				return ApiResult.result(ApiCode.COUNT_EXCEPTION, "有效号码数不足2000或大于2000000【" + fileContent.getMobileCounts() + "】", null);
			}

			mobileCount = fileContent.getMobileCounts();
			// 3.预扣账户余额
			Boolean isFreeze = balanceService.preDeductFee(customerId, mobileCount,
					DirectTypeEnum.getProductEnumByName(productType).getProductCode(), intDirectId);
			if (!isFreeze) {
				lock.releaseLock();
				redisClient.set(String.format(IntDirectRedisKeyConstant.EXCEPTION_KEY, customerId, intDirectId),
						CommonConstant.FILE_TEST_FAILED_CODE, expire);
				return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足或预扣失败", null);
			}
						
			// 4.调用聚赢上传接口
			String uploadFilePath = intDirectFileService.getTxtPath(uploadPath, IntDirectTxtSuffixEnum.ALL);
			UploadResponse uploadResponse = internationalService.directUpload(customerId, 
					uploadFilePath.substring(uploadFilePath.lastIndexOf("/")).replace("/", "").replace(".txt", ""),
					uploadFilePath,productType);
			if (uploadResponse == null) {
				lock.releaseLock();
				// 返还预扣费
				balanceService.backDeductFee(customerId, mobileCount, DirectTypeEnum.getProductEnumByName(productType).getProductCode(),
						intDirectId);
				redisClient.set(String.format(IntDirectRedisKeyConstant.EXCEPTION_KEY, customerId, intDirectId),
						CommonConstant.FILE_TEST_FAILED_CODE, expire);
				return ApiResult.result(ApiCode.BUSINESS_EXCEPTION, "上游接口调用异常", null);
			}
			
			// 5.提交的有效号码总个数存入redis
			String addFlag = redisClient.set(String.format(IntDirectRedisKeyConstant.SUCCEED_CLEARING_COUNT_KEY,
					customerId, intDirectId), String.valueOf(mobileCount), expire);
			if (StringUtils.isBlank(addFlag) || !CommonConstant.REDIS_SET_RETURN.equals(addFlag)) {
				lock.releaseLock();
				redisClient.set(String.format(IntDirectRedisKeyConstant.EXCEPTION_KEY, customerId, intDirectId),
						CommonConstant.FILE_TEST_FAILED_CODE, expire);
				// 返还预扣费
				balanceService.backDeductFee(customerId, mobileCount, DirectTypeEnum.getProductEnumByName(productType).getProductCode(),
						intDirectId);
				return ApiResult.result(ApiCode.COUNT_EXCEPTION, "计数异常", null);
			}
			
			sendID = uploadResponse.getSendID();
			mobileCount = Integer.valueOf(uploadResponse.getLine());
			// 6.初始化定向国际检测相关配置redis值
			fileRedisService.intDirectRedisInit(customerId, intDirectId, expire, lock.getIdentifier(),
					Long.valueOf(mobileCount));
			// 7.保存临时文件
			intDirectFileService.saveTempFileByAll(uploadPath, customerId, fileContent.getMobileList(), intDirectId);
			
			// 8.执行定向检测进度查询定时任务
			directCheckProgressService.timingExecute(getTaskInfo(countryCode, customerId, sendID, uploadPath, intDirectId, productType, sourceFileName,mobileCount));
			
			log.info("{}, 定向国际检测文件提交到下游成功，countryCode：{}，productType：{}，sendID：{}, intDirectId:{}",customerId,countryCode,productType,sendID,intDirectId);
		} catch (Exception e) {
			lock.releaseLock();
			// 返还预扣费
			balanceService.backDeductFee(customerId, mobileCount, DirectTypeEnum.getProductEnumByName(productType).getProductCode(),
					intDirectId);

			log.error("{}, 用户定向国际在线检测异常，intDirectId:{},info:", customerId, intDirectId, e);
			dingDingMessage.sendMessage(String.format("警告： 用户【%s】定向国际在线检测生成临时文件异常，intDirectId:%s，info:%s", customerId,
					intDirectId, e));
			return ApiResult.fail("定向国际检测异常");
		}

		lock.releaseLock();
		return ApiResult.ok(new InternationalRunTestDomian(mobileCount, CommonConstant.THETEST_RUNNING,
				intDirectId.toString(), sendID));
	}
	
	public ApiResult queryIntDirectProcess(ProgressTaskInfo progressTaskInfo) {
		// 调用上游接口查询检测进度
		QueryResponse queryResponse = internationalService.query(progressTaskInfo.getCustomerId(), progressTaskInfo.getExternFileId());
		if(queryResponse == null) {
			return ApiResult.fail(ApiCode.BUSINESS_EXCEPTION,"检测进度查询失败");
		}
		
		// 成功检测条数累加
		if(StringUtils.isBlank(progressTaskInfo.getProductType())) {
			redisClient.set(String.format(InternationalRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId()), 
					String.valueOf(Integer.valueOf(queryResponse.getNumber2())+Integer.valueOf(queryResponse.getNumber3())), 3 * 60 * 60 * 1000);
		}else {
			redisClient.set(String.format(IntDirectRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId()), 
					String.valueOf(Integer.valueOf(queryResponse.getNumber2())+Integer.valueOf(queryResponse.getNumber3())), 3 * 60 * 60 * 1000);
		}
				
		if("2".equals(queryResponse.getStatus())) {
			// 检测完成，处理结果文件
			if(StringUtils.isBlank(progressTaskInfo.getProductType())) {
				generateResultFiles(queryResponse, progressTaskInfo);
			}else {
				generateIntDirectResultFiles(queryResponse, progressTaskInfo);
			}
		}
		
		log.info("{}, {}进度查询成功，id：{}，sendID:{},sourceFileName:{},response:{}",progressTaskInfo.getCustomerId(),StringUtils.isBlank(progressTaskInfo.getProductType())?"国际在线检测":"定向国际在线检测",
				progressTaskInfo.getIntDirectId(),progressTaskInfo.getExternFileId(),progressTaskInfo.getSourceFileName(),JSON.toJSONString(queryResponse));
		return ApiResult.ok(queryResponse);
	}

	/**
	 * 生成结果报表
	 */
	@Transactional
	private void generateResultFiles(QueryResponse queryResponse, ProgressTaskInfo progressTaskInfo) {
		try {
			// 校验是否已经处理成功
			String testCode = redisClient.get(String.format(InternationalRedisKeyConstant.THE_RUN_KEY, progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()));
			if(StringUtils.isBlank(testCode) || CommonConstant.FILE_TEST_FAILED_CODE.equals(testCode)) {
				log.info("该文件已经检测完成，不再做处理，info:{}",JSON.toJSONString(progressTaskInfo));
				return ;
			}
			
			// 查询检测记录
			InternationalCheck internationalCheck = internationalCheckService.findOne(progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId());
			if (internationalCheck == null) {
				log.error("{}, 国际文件检测失败，检测记录查无记录，intDirectId:{},sendID:{}",progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId(),progressTaskInfo.getExternFileId());
				dingDingMessage.sendMessage(
						String.format("警告：%s, 国际在线检测失败，检测记录查无记录，intDirectId:%s,sendID:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId(),progressTaskInfo.getExternFileId()));
				return;
			}
			
			InternationalCvsFilePath internationalCvsFilePath = fileService.getTestResultByTxtFile(queryResponse, progressTaskInfo);
			if(null == internationalCvsFilePath) {
				return ;
			}
			
			int counts = internationalCvsFilePathService.saveOne(internationalCvsFilePath);
			if (counts != 1) {
				log.error("{}, 国际文件检测成功，但文件结果包信息记录插入失败，info:{}", progressTaskInfo.getCustomerId(), JSON.toJSONString(internationalCvsFilePath));
				dingDingMessage.sendMessage(
						String.format("警告：%s, 国际在线检测成功，但文件结果包信息记录插入失败，id:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()));
				throw new RuntimeException("国际检测文件结果包信息插入失败");
			}

			// 测数据保存到数据库，同时更新国际检测订单状态
			InternationalCheck temp = new InternationalCheck();
			temp.setId(progressTaskInfo.getIntDirectId()).setStatus(IntDirectCheck.IntDirectCheckStatus.WORK_FINISH.getStatus()).setExternFileId(progressTaskInfo.getExternFileId())
					.setActiveCount(Long.valueOf(internationalCvsFilePath.getActiveNumber()==null?0:internationalCvsFilePath.getActiveNumber()))
					.setNoRegisterCount(Long.valueOf(internationalCvsFilePath.getNoRegisterNumber()==null?0:internationalCvsFilePath.getNoRegisterNumber()))
					.setUnknownCount(Long.valueOf(internationalCvsFilePath.getUnknownNumber()==null?0:internationalCvsFilePath.getUnknownNumber()))
					.setTotalNumber(Long.valueOf(internationalCvsFilePath.getTotalNumber()==null?0:internationalCvsFilePath.getTotalNumber()));
			counts = internationalCheckService.updateOne(temp);
			if (counts != 1) {
				log.error("{}, 国际文件检测成功，但检测记录更新失败，info:{}", progressTaskInfo.getCustomerId(), JSON.toJSONString(temp));
				dingDingMessage
						.sendMessage(String.format("警告：%s, 国际在线检测成功，但检测记录更新失败，intDirectId:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()));
				throw new RuntimeException("国际检测记录更新失败");
			}

			CustomerConsume consume = new CustomerConsume();
			consume.setEmptyId(progressTaskInfo.getIntDirectId()).setCustomerId(progressTaskInfo.getCustomerId())
					.setConsumeNumber(Long.valueOf(internationalCvsFilePath.getTotalNumber()==null?0:internationalCvsFilePath.getTotalNumber()))
					.setConsumeType(CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue())
					.setClosingBalance(0L)
					.setOpeningBalance(consume.getClosingBalance() + consume.getConsumeNumber());
			counts = customerConsumeService.updateOne(consume);
			if (counts != 1) {
				log.error("{}, 国际文件检测成功，但消耗记录更新失败，info:{}", progressTaskInfo.getCustomerId(), JSON.toJSONString(consume));
				dingDingMessage
						.sendMessage(String.format("警告：%s, 国际在线检测成功，但消耗记录更新失败，intDirectId:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()));
				throw new RuntimeException("国际消耗记录更新失败");
			}
			
			//执行扣费操作
            balanceService.deductFee(progressTaskInfo.getCustomerId(), progressTaskInfo.getMobileCount(), 
            		internationalCvsFilePath.getTotalNumber(), ProductEnum.INTERNATIONAL.getProductCode(),progressTaskInfo.getIntDirectId());

			// 设置程序运行结束
			redisClient.set(String.format(InternationalRedisKeyConstant.THE_RUN_KEY, progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()),
					CommonConstant.FILE_TEST_FAILED_CODE,  3 * 60 * 60 * 1000);
			// 文件md5存入缓存
			fileUploadService.handleFileMd5Cache(progressTaskInfo.getIntDirectId(), progressTaskInfo.getCustomerId());
			log.info("----------用户编号：[{}]结束执行国际检测检索事件,intDirectId:{}----------", progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId());
		} catch (Exception e) {
			log.error("{},线程执行国际检测异常,intertionalId:{},info:", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId(), e);
			dingDingMessage.sendMessage(
					String.format("警告：%s,线程执行国际检测生成文件异常,intertionalId:%s,info:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId(), e));
		}
	}
	
	@Transactional
	private void generateIntDirectResultFiles(QueryResponse queryResponse, ProgressTaskInfo progressTaskInfo) {
		try {
			// 校验是否已经处理成功
			String testCode = redisClient.get(String.format(IntDirectRedisKeyConstant.THE_RUN_KEY, progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()));
			if(StringUtils.isBlank(testCode) || CommonConstant.FILE_TEST_FAILED_CODE.equals(testCode)) {
				log.info("该文件已经检测完成，不再做处理，info:{}",JSON.toJSONString(progressTaskInfo));
				return ;
			}
			
			// 查询检测记录
			IntDirectCheck intDirectCheck = intDirectCheckService.findOne(progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId());
			if (intDirectCheck == null) {
				log.error("{}, 定向国际文件检测失败，检测记录查无记录，intDirectId:{},sendID:{}",progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId(),progressTaskInfo.getExternFileId());
				dingDingMessage.sendMessage(
						String.format("警告：%s, 定向国际在线检测失败，检测记录查无记录，intDirectId:%s,sendID:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId(),progressTaskInfo.getExternFileId()));
				return;
			}
			
			IntDirectCvsFilePath intDirectCvsFilePath = intDirectFileService.getTestResultByTxtFile(queryResponse, progressTaskInfo);
			if(null == intDirectCvsFilePath) {
				return ;
			}
			
			int counts = intDirectCvsFilePathService.saveOne(intDirectCvsFilePath);
			if (counts != 1) {
				log.error("{}, 定向国际文件检测成功，但文件结果包信息记录插入失败，info:{}", progressTaskInfo.getCustomerId(), JSON.toJSONString(intDirectCvsFilePath));
				dingDingMessage.sendMessage(
						String.format("警告：%s, 定向国际在线检测成功，但文件结果包信息记录插入失败，intDirectId:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()));
				throw new RuntimeException("定向国际检测文件结果包信息插入失败");
			}

			// 测数据保存到数据库，同时更新国际检测订单状态
			IntDirectCheck temp = new IntDirectCheck();
			temp.setId(progressTaskInfo.getIntDirectId()).setStatus(IntDirectCheck.IntDirectCheckStatus.WORK_FINISH.getStatus()).setExternFileId(progressTaskInfo.getExternFileId())
					.setActiveCount(Long.valueOf(intDirectCvsFilePath.getActiveNumber()==null?0:intDirectCvsFilePath.getActiveNumber()))
					.setNoRegisterCount(Long.valueOf(intDirectCvsFilePath.getNoRegisterNumber()==null?0:intDirectCvsFilePath.getNoRegisterNumber()))
					.setTotalNumber(Long.valueOf(intDirectCvsFilePath.getTotalNumber()==null?0:intDirectCvsFilePath.getTotalNumber()));
			counts = intDirectCheckService.updateOne(temp);
			if (counts != 1) {
				log.error("{}, 定向国际文件检测成功，但检测记录更新失败，info:{}", progressTaskInfo.getCustomerId(), JSON.toJSONString(temp));
				dingDingMessage
						.sendMessage(String.format("警告：%s, 定向国际在线检测成功，但检测记录更新失败，intDirectId:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()));
				throw new RuntimeException("定向国际检测记录更新失败");
			}

			CustomerConsume consume = new CustomerConsume();
			consume.setEmptyId(progressTaskInfo.getIntDirectId()).setCustomerId(progressTaskInfo.getCustomerId())
					.setConsumeNumber(Long.valueOf(intDirectCvsFilePath.getTotalNumber()==null?0:intDirectCvsFilePath.getTotalNumber()))
					.setConsumeType(CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue())
					.setClosingBalance(0L)
					.setOpeningBalance(consume.getClosingBalance() + consume.getConsumeNumber());
			counts = customerConsumeService.updateOne(consume);
			if (counts != 1) {
				log.error("{}, 定向国际文件检测成功，但消耗记录更新失败，info:{}", progressTaskInfo.getCustomerId(), JSON.toJSONString(consume));
				dingDingMessage
						.sendMessage(String.format("警告：%s, 定向国际在线检测成功，但消耗记录更新失败，intDirectId:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()));
				throw new RuntimeException("定向国际消耗记录更新失败");
			}
			
			// 扣费
			balanceService.deductFee(progressTaskInfo.getCustomerId(), progressTaskInfo.getMobileCount(), 
					intDirectCvsFilePath.getTotalNumber()==null?0:intDirectCvsFilePath.getTotalNumber(), 
				    DirectTypeEnum.getProductEnumByName(progressTaskInfo.getProductType()).getProductCode(), progressTaskInfo.getIntDirectId());

			// 设置程序运行结束
			redisClient.set(String.format(IntDirectRedisKeyConstant.THE_RUN_KEY, progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()),
					CommonConstant.FILE_TEST_FAILED_CODE,  3 * 60 * 60 * 1000);
			// 文件md5存入缓存
			fileUploadService.handleFileMd5Cache(progressTaskInfo.getIntDirectId(), progressTaskInfo.getCustomerId());
			log.info("----------用户编号：[{}]结束执行定向国际检测检索事件,intDirectId:{}----------", progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId());
		} catch (Exception e) {
			log.error("{},线程执行定向国际检测异常,intDirectId:{},info:", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId(), e);
			dingDingMessage.sendMessage(
					String.format("警告：%s,线程执行定向国际检测生成文件异常,realtimeId:%s,info:%s", progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId(), e));
		}
	}
	
	private ProgressTaskInfo getTaskInfo(String countryCode,Long customerId,String externFileId,String fileUrl,Long intDirectId,String productType,String sourceFileName,Integer mobileCount) {
		ProgressTaskInfo progressTaskInfo = new ProgressTaskInfo();
		progressTaskInfo.setCountryCode(countryCode);
		progressTaskInfo.setCustomerId(customerId);
		progressTaskInfo.setExternFileId(externFileId);
		progressTaskInfo.setFileUrl(fileUrl);
		progressTaskInfo.setIntDirectId(intDirectId);
		progressTaskInfo.setProductType(productType);
		progressTaskInfo.setSourceFileName(sourceFileName);
		progressTaskInfo.setMobileCount(mobileCount);
		return progressTaskInfo;
	}
}

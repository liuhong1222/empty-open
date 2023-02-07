package com.zhongzhi.empty.service.realtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.entity.CustomerConsume;
import com.zhongzhi.empty.entity.RealtimeCheck;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.enums.MobileStatusQueryStateEnum;
import com.zhongzhi.empty.enums.MobileStatusStardardDxStateEnum;
import com.zhongzhi.empty.enums.MobileStatusStardardLdStateEnum;
import com.zhongzhi.empty.enums.MobileStatusStardardYdStateEnum;
import com.zhongzhi.empty.enums.ProductEnum;
import com.zhongzhi.empty.enums.RealtimeCheckTypeEnum;
import com.zhongzhi.empty.enums.UserCheckTypeEnum;
import com.zhongzhi.empty.http.realtime.MobileRealtimeStatus;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.LuaByDeductFeeResponse;
import com.zhongzhi.empty.response.MobileStatusStaticResult;
import com.zhongzhi.empty.response.RealtimeResult;
import com.zhongzhi.empty.service.balance.BalanceService;
import com.zhongzhi.empty.service.gateway.RealtimeService;
import com.zhongzhi.empty.task.CustomerConsumeLocalCache;
import com.zhongzhi.empty.task.RealtimeCheckLocalCache;
import com.zhongzhi.empty.util.Snowflake;
import com.zhongzhi.empty.util.ThreadLocalContainer;
import com.zhongzhi.empty.vo.CustomerInfoVo;

import lombok.extern.slf4j.Slf4j;

/**
 * 号码实时检测对外接口api
 * @author liuh
 * @date 2021年11月2日
 */
@Slf4j
@Service
public class RealtimeApiService {
	
	@Autowired
	private BalanceService balanceService;
	
	@Autowired
	private RealtimeService realtimeService;
	
	@Autowired
	private Snowflake snowflake;
	
	private static RealtimeCheckLocalCache realtimeCheckLocalCache = RealtimeCheckLocalCache.getInStance();
	
	private static CustomerConsumeLocalCache customerConsumeLocalCache = CustomerConsumeLocalCache.getInStance();
	
	private static ExecutorService executor = Executors.newFixedThreadPool(20);

	public ApiResult<MobileStatusStaticResult> mobileStatusStaticQuery(Set<String> mobileList,String ip){
		long st = System.currentTimeMillis();
		// 线程缓存里获取用户信息
		CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
		Long id = snowflake.nextId();
		try {
			// 预扣费
			boolean preDeductFeeFlag = balanceService.preDeductFee(customerInfoVo.getCustomerId(), 
					mobileList.size(), ProductEnum.REALTIME.getProductCode(),id);
			if(!preDeductFeeFlag) {
		         return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足", null);
			}
			
			int chargeCount = 0;
			List<RealtimeResult> resultList = new ArrayList<RealtimeResult>();
			List<CompletableFuture<RealtimeResult>> cfts = new CopyOnWriteArrayList<>();
			mobileList.forEach(mobile->{
				CompletableFuture<RealtimeResult> cft = CompletableFuture.supplyAsync(() -> {
					// 调用业务接口
					MobileRealtimeStatus mobileRealtimeStatus = realtimeService.mobileStatusStaticQuery(customerInfoVo.getCustomerId(), mobile);
					if (mobileRealtimeStatus == null) {
						return null;
					}
					
					boolean chargeFlag = true;
					if(Arrays.asList(CommonConstant.REALTIME_FREE_CHARGE_CODE.split(",")).contains(mobileRealtimeStatus.getStatus())) {
						if("9".equals(mobileRealtimeStatus.getStatus())) {
							mobileRealtimeStatus.setStatus("10");
						}
						
						chargeFlag = false;
					}
					
					return getRealtimeResultData(mobileRealtimeStatus,chargeFlag);
				}, executor);
				cfts.add(cft);
			});
			
			Set<RealtimeResult> tempSet = cfts.parallelStream().map(futrue -> {
				try {
					return futrue.get(10, TimeUnit.SECONDS);
				} catch (Exception e) {
					log.error("{}，CompletableFuture get timeout：",customerInfoVo.getCustomerId(),e);
					return new RealtimeResult();
				}
			}).collect(Collectors.toSet());
			
			if(tempSet == null) {
	        	//返还预扣费
	        	balanceService.backDeductFee(customerInfoVo.getCustomerId(), mobileList.size(), ProductEnum.REALTIME.getProductCode(),id);
	        	
	        	log.error("{}, 号码实时查询基础版接口调用失败，调用下游接口失败或超时,mobile:{}",customerInfoVo.getCustomerId(), JSON.toJSONString(mobileList));
	            return ApiResult.result(ApiCode.FAIL, "系统异常", null);
	        }
			
			List<RealtimeResult> resultSet = new ArrayList<RealtimeResult>();			
			for(RealtimeResult temp : tempSet) {
				if(temp != null) {
					chargeCount += temp.getChargeStatus();
					resultSet.add(temp);
				}
			}
						
			// 扣费
	        LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerInfoVo.getCustomerId(), 
	        		mobileList.size(), chargeCount, ProductEnum.REALTIME.getProductCode(),id);
	        
	        // 检测记录入库
	        RealtimeCheck realtimeCheck = getRealtimeCheckDataByBatch(resultSet, customerInfoVo, id,chargeCount,UserCheckTypeEnum.API.getName());
	        realtimeCheckLocalCache.setLocalCache(realtimeCheck);
	         
	        // 消费记录入库
	        CustomerConsume consume = getCustomerConsumeData(customerInfoVo, realtimeCheck, luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance(),
	        		CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue(),chargeCount,ProductEnum.REALTIME.getProductCode());
	        customerConsumeLocalCache.setLocalCache(consume);
	        
	        MobileStatusStaticResult mobileStatusStaticResult = new MobileStatusStaticResult();
	        mobileStatusStaticResult.setChargeCount(chargeCount);
	        mobileStatusStaticResult.setMobiles(resultSet);
	        
	        log.info("{}, 号码实时查询基础版接口调用成功,ip:{},useTime:{}",customerInfoVo.getCustomerId(),ip,(System.currentTimeMillis()-st));
	        return ApiResult.ok(mobileStatusStaticResult);
		} catch (Exception e) {
			log.error("{}, 号码实时查询基础版接口调用异常，mobile:{},ip:{},info:",customerInfoVo.getCustomerId(),JSON.toJSONString(mobileList),ip,e);
			return ApiResult.result(ApiCode.FAIL, "系统异常", null);
		}
	}
	
	public ApiResult<RealtimeResult> mobileStatusStaticQueryNew(String mobile,String ip){
		long st = System.currentTimeMillis();
		// 线程缓存里获取用户信息
		CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
		Long id = snowflake.nextId();
		try {
			// 预扣费
			boolean preDeductFeeFlag = balanceService.preDeductFee(customerInfoVo.getCustomerId(), 
						1, ProductEnum.REALTIME.getProductCode(),id);
			if(!preDeductFeeFlag) {
		         return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足", null);
			}
			
			// 调用业务接口
			MobileRealtimeStatus mobileRealtimeStatus = realtimeService.mobileStatusStaticQuery(customerInfoVo.getCustomerId(), mobile);
			if(mobileRealtimeStatus == null) {
	        	//返还预扣费
	        	balanceService.backDeductFee(customerInfoVo.getCustomerId(), 1, ProductEnum.REALTIME.getProductCode(),id);
	        	
	        	log.error("{}, 号码实时查询基础版新接口调用失败，调用下游接口失败,mobile:{}",customerInfoVo.getCustomerId(), mobile);
	            return ApiResult.result(ApiCode.FAIL, "系统异常", null);
	        }
			
			boolean chargeFlag = true;
			if(Arrays.asList(CommonConstant.REALTIME_FREE_CHARGE_CODE.split(",")).contains(mobileRealtimeStatus.getStatus())) {
				if("9".equals(mobileRealtimeStatus.getStatus())) {
					mobileRealtimeStatus.setStatus("10");
				}
				
				chargeFlag = false;
			}
			
			// 扣费
	        LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerInfoVo.getCustomerId(), 
	        	1, chargeFlag?1:0, ProductEnum.REALTIME.getProductCode(),id);
	        
	        // 检测记录入库
	        RealtimeCheck realtimeCheck = getRealtimeCheckDataByApi(mobileRealtimeStatus, customerInfoVo, id,chargeFlag,UserCheckTypeEnum.API.getName());
	        realtimeCheckLocalCache.setLocalCache(realtimeCheck);
	         
	        // 消费记录入库
	        CustomerConsume consume = getCustomerConsumeData(customerInfoVo, realtimeCheck, luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance(),
	        		CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue(),chargeFlag?1:0,ProductEnum.REALTIME.getProductCode());
	        customerConsumeLocalCache.setLocalCache(consume);
	        
	        log.info("{}, 号码实时查询基础版新接口调用成功,ip:{},useTime:{}",customerInfoVo.getCustomerId(),ip,(System.currentTimeMillis()-st));
	        return ApiResult.ok(getRealtimeResultDataByStardard(mobileRealtimeStatus,chargeFlag));
		} catch (Exception e) {
			log.error("{}, 号码实时查询基础版新接口调用异常，mobile:{},ip:{},info:",customerInfoVo.getCustomerId(),mobile,ip,e);
			return ApiResult.result(ApiCode.FAIL, "系统异常", null);
		}
	}
	
	public ApiResult<RealtimeResult> mobileStatusStardard(String mobile,String ip){
		long st = System.currentTimeMillis();
		// 线程缓存里获取用户信息
		CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
		Long id = snowflake.nextId();
		try {
			// 预扣费
			boolean preDeductFeeFlag = balanceService.preDeductFee(customerInfoVo.getCustomerId(), 
						1, ProductEnum.REALTIME.getProductCode(),id);
			if(!preDeductFeeFlag) {
		         return ApiResult.result(ApiCode.BALANCE_EXCEPTION, "余额不足", null);
			}
			
			MobileRealtimeStatus mobileRealtimeStatus = realtimeService.mobileStatusQuery(customerInfoVo.getCustomerId(), mobile);
			if(mobileRealtimeStatus == null) {
	        	//返还预扣费
	        	balanceService.backDeductFee(customerInfoVo.getCustomerId(), 1, ProductEnum.REALTIME.getProductCode(),id);
	        	
	        	log.error("{}, 号码实时查询接口失败，调用下游接口失败,mobile:{}",customerInfoVo.getCustomerId(), mobile);
	            return ApiResult.result(ApiCode.FAIL, "系统异常", null);
	        }
			
			boolean chargeFlag = true;
			if(Arrays.asList(CommonConstant.REALTIME_FREE_CHARGE_CODE.split(",")).contains(mobileRealtimeStatus.getStatus().toString())) {
				chargeFlag = false;
			}
			
			// 扣费
	        LuaByDeductFeeResponse luaByDeductFeeResponse = balanceService.deductFee(customerInfoVo.getCustomerId(), 
	        	1, chargeFlag?1:0, ProductEnum.REALTIME.getProductCode(),id);
	        
	        // 检测记录入库
	        RealtimeCheck realtimeCheck = getRealtimeCheckDataByStardard(mobileRealtimeStatus, customerInfoVo, id,chargeFlag,UserCheckTypeEnum.API_STARDARD.getName());
	        realtimeCheckLocalCache.setLocalCache(realtimeCheck);
	         
	        // 消费记录入库
	        CustomerConsume consume = getCustomerConsumeData(customerInfoVo, realtimeCheck, luaByDeductFeeResponse==null?0L:luaByDeductFeeResponse.getBalance(),
	        		CustomerConsume.ConsumeType.DEDUCTION_SUCCESS.getValue(),chargeFlag?1:0,ProductEnum.REALTIME_STARDARD.getProductCode());
	        customerConsumeLocalCache.setLocalCache(consume);
	        	        
	        log.info("{}, 号码实时查询接口调用成功,ip:{},useTime:{}",customerInfoVo.getCustomerId(),ip,(System.currentTimeMillis()-st));
	        return ApiResult.ok(getRealtimeResultDataByStardard(mobileRealtimeStatus,chargeFlag));
		} catch (Exception e) {
			log.error("{}, 号码实时查询接口调用异常，mobile:{},ip:{},info:",customerInfoVo.getCustomerId(),mobile,ip,e);
			return ApiResult.result(ApiCode.FAIL, "系统异常", null);
		}		
	}
	
	private RealtimeResult getRealtimeResultData(MobileRealtimeStatus mobileRealtimeStatus,boolean chargeFlag){
		RealtimeResult realtimeResult = new RealtimeResult();
		realtimeResult.setOrderNo(mobileRealtimeStatus.getOrderNo());
		realtimeResult.setHandleTime(mobileRealtimeStatus.getHandleTime());
		realtimeResult.setArea(mobileRealtimeStatus.getArea());
		realtimeResult.setCarrier(mobileRealtimeStatus.getNumberType());
		realtimeResult.setMnpStatus(mobileRealtimeStatus.getMnpStatus());
		realtimeResult.setMobile(mobileRealtimeStatus.getMobile());
		realtimeResult.setRemark(mobileRealtimeStatus.getRemark());
		realtimeResult.setStatus(Integer.valueOf(mobileRealtimeStatus.getStatus()));
		realtimeResult.setChargeStatus(chargeFlag?1:0);
		return realtimeResult;
	}
	
	private RealtimeResult getRealtimeResultDataByStardard(MobileRealtimeStatus mobileRealtimeStatus,boolean chargeFlag){
		RealtimeResult realtimeResult = new RealtimeResult();
		realtimeResult.setOrderNo(mobileRealtimeStatus.getOrderNo());
		realtimeResult.setHandleTime(mobileRealtimeStatus.getHandleTime());
		realtimeResult.setArea(mobileRealtimeStatus.getArea());
		realtimeResult.setCarrier(mobileRealtimeStatus.getNumberType());
		realtimeResult.setChargeStatus(chargeFlag?1:0);
		realtimeResult.setMnpStatus(mobileRealtimeStatus.getMnpStatus());
		realtimeResult.setMobile(mobileRealtimeStatus.getMobile());
		realtimeResult.setRemark(mobileRealtimeStatus.getRemark());
		realtimeResult.setStatus(Integer.valueOf(mobileRealtimeStatus.getStatus()));
		return realtimeResult;
	}
	
	private RealtimeCheck getRealtimeCheckDataByBatch(List<RealtimeResult> list,CustomerInfoVo customerInfoVo,Long id,int chargeCount,String remark) {
		//分组汇总统计
		Map<Integer,Long> map = summyRealtimeResult(list);
		
		RealtimeCheck realtimeCheck = new RealtimeCheck();
		realtimeCheck.setStatus(RealtimeCheck.RealtimeCheckStatus.WORK_FINISH.getStatus())
        .setSize("0")
        .setFileUrl("")
        .setAgentId(customerInfoVo.getAgentId())
        .setAgentName(customerInfoVo.getCompanyName())
        .setCustomerId(customerInfoVo.getCustomerId())
        .setName(list.get(0).getMobile())
        .setNormal(map.get(MobileStatusQueryStateEnum.NORMAL.getCode()))
        .setEmpty(map.get(MobileStatusQueryStateEnum.EMPTY.getCode()))
        .setOnCall(map.get(MobileStatusQueryStateEnum.ON_CALL.getCode()))
        .setOnlineButNotAvailable(map.get(MobileStatusQueryStateEnum.ONLINE_BUT_NOT_AVAILABLE.getCode()))
        .setShutdown(map.get(MobileStatusQueryStateEnum.SHUTDOWN.getCode()))
        .setCallTransfer(0L)
        .setSuspectedShutdown(map.get(MobileStatusQueryStateEnum.SUSPECTED_SHUTDOWN.getCode()))
        .setServiceSuspended(map.get(MobileStatusQueryStateEnum.SERVICE_SUSPENDED.getCode()))
        .setNumberPortability(map.get(MobileStatusQueryStateEnum.NUMBER_PORTABILITY.getCode()))
        .setUnknown(map.get(MobileStatusQueryStateEnum.NUMBER_ERROR.getCode()))
        .setIllegalNumber(Long.valueOf(list.size() - chargeCount))
        .setExceptionFailCount((map.get(MobileStatusQueryStateEnum.SERVER_EXCEPTION.getCode())==null?0L:map.get(MobileStatusQueryStateEnum.SERVER_EXCEPTION.getCode()))
        		+(map.get(MobileStatusQueryStateEnum.EXCEPTION_FAIL.getCode())==null?0L:map.get(MobileStatusQueryStateEnum.EXCEPTION_FAIL.getCode())))
        .setTotalNumber(Long.valueOf(list.size()))
        .setId(id)
        .setRetryCount(0)
        .setDeleted(0)
        .setVersion(0)
        .setCheckType(RealtimeCheckTypeEnum.CHUANGLAN.getCode())
        .setRemark(remark);		
		return realtimeCheck;
	}
	
	private Map<Integer,Long> summyRealtimeResult(List<RealtimeResult> list){
		Map<Integer,Long> result = new HashMap<Integer, Long>();
		for(RealtimeResult realtimeResult : list) {
			if(MobileStatusQueryStateEnum.NORMAL.getCode()==realtimeResult.getStatus()) {
				if("1".equals(realtimeResult.getMnpStatus())) {
					if(result.containsKey(MobileStatusQueryStateEnum.NUMBER_PORTABILITY.getCode())) {
						result.put(MobileStatusQueryStateEnum.NUMBER_PORTABILITY.getCode(), result.get(11) + 1L);
					}else {
						result.put(MobileStatusQueryStateEnum.NUMBER_PORTABILITY.getCode(), 1L);
					}
				}else {
					if(result.containsKey(realtimeResult.getStatus())) {
						result.put(realtimeResult.getStatus(), result.get(realtimeResult.getStatus()) + 1L);
					}else {
						result.put(realtimeResult.getStatus(), 1L);
					}
				}
			}else {
				if(result.containsKey(realtimeResult.getStatus())) {
					result.put(realtimeResult.getStatus(), result.get(realtimeResult.getStatus()) + 1L);
				}else {
					result.put(realtimeResult.getStatus(), 1L);
				}
			}
		}
		return result;
	}
	
	private RealtimeCheck getRealtimeCheckDataByApi(MobileRealtimeStatus mobileRealtimeStatus,CustomerInfoVo customerInfoVo,Long id,boolean chargeFlag,String remark) {
		RealtimeCheck realtimeCheck = new RealtimeCheck();
		realtimeCheck.setStatus(RealtimeCheck.RealtimeCheckStatus.WORK_FINISH.getStatus())
        .setSize("0")
        .setFileUrl("")
        .setAgentId(customerInfoVo.getAgentId())
        .setAgentName(customerInfoVo.getCompanyName())
        .setCustomerId(customerInfoVo.getCustomerId())
        .setName(mobileRealtimeStatus.getMobile())
        .setNormal(0L)
        .setEmpty(MobileStatusQueryStateEnum.EMPTY.getCode()==Integer.valueOf(mobileRealtimeStatus.getStatus())?1L:0L)
        .setOnCall(MobileStatusQueryStateEnum.ON_CALL.getCode()==Integer.valueOf(mobileRealtimeStatus.getStatus())?1L:0L)
        .setOnlineButNotAvailable(MobileStatusQueryStateEnum.ONLINE_BUT_NOT_AVAILABLE.getCode()==Integer.valueOf(mobileRealtimeStatus.getStatus())?1L:0L)
        .setShutdown(MobileStatusQueryStateEnum.SHUTDOWN.getCode()==Integer.valueOf(mobileRealtimeStatus.getStatus())?1L:0L)
        .setCallTransfer(0L)
        .setSuspectedShutdown(MobileStatusQueryStateEnum.SUSPECTED_SHUTDOWN.getCode()==Integer.valueOf(mobileRealtimeStatus.getStatus())?1L:0L)
        .setServiceSuspended(MobileStatusQueryStateEnum.SERVICE_SUSPENDED.getCode()==Integer.valueOf(mobileRealtimeStatus.getStatus())?1L:0L)
        .setNumberPortability(0L)
        .setUnknown(MobileStatusQueryStateEnum.NUMBER_ERROR.getCode()==Integer.valueOf(mobileRealtimeStatus.getStatus())?1L:0L)
        .setIllegalNumber(chargeFlag?0L:1L)
        .setExceptionFailCount(Arrays.asList(CommonConstant.REALTIME_FREE_CHARGE_CODE.split(",")).contains(mobileRealtimeStatus.getStatus())?1L:0L)
        .setTotalNumber(1L)
        .setId(id)
        .setRetryCount(0)
        .setDeleted(0)
        .setVersion(0)
        .setCheckType(RealtimeCheckTypeEnum.CHUANGLAN.getCode())
        .setRemark(remark);
		
		if(MobileStatusQueryStateEnum.NORMAL.getCode()==Integer.valueOf(mobileRealtimeStatus.getStatus())) {
			if("1".equals(mobileRealtimeStatus.getMnpStatus())) {
				realtimeCheck.setNumberPortability(1L);
			}else {
				realtimeCheck.setNormal(1L);
			}
		}
		
		return realtimeCheck;
	}
	
	private RealtimeCheck getRealtimeCheckDataByStardard(MobileRealtimeStatus mobileRealtimeStatus,CustomerInfoVo customerInfoVo,Long id,boolean chargeFlag,String remark) {
		RealtimeCheck realtimeCheck = new RealtimeCheck();
		realtimeCheck.setStatus(RealtimeCheck.RealtimeCheckStatus.WORK_FINISH.getStatus())
        .setSize("0")
        .setFileUrl("")
        .setAgentId(customerInfoVo.getAgentId())
        .setAgentName(customerInfoVo.getCompanyName())
        .setCustomerId(customerInfoVo.getCustomerId())
        .setName(mobileRealtimeStatus.getMobile())
        .setCallTransfer(0L)
        .setIllegalNumber(chargeFlag?0L:1L)
        .setExceptionFailCount(Arrays.asList(CommonConstant.REALTIME_FREE_CHARGE_CODE.split(",")).contains(mobileRealtimeStatus.getStatus())?1L:0L)
        .setTotalNumber(1L)
        .setId(id)
        .setRetryCount(0)
        .setDeleted(0)
        .setVersion(0)
        .setCheckType(RealtimeCheckTypeEnum.CHUANGLAN.getCode())
        .setNumberPortability(0L)
        .setOnCall(0L)
        .setNormal(0L)
        .setEmpty(0L)
        .setOnlineButNotAvailable(0L)
        .setShutdown(0L)
        .setSuspectedShutdown(0L)
        .setServiceSuspended(0L)
        .setUnknown(0L)
        .setRemark(remark);
		
		return transStatus(realtimeCheck, mobileRealtimeStatus);
	}
	
	private RealtimeCheck transStatus(RealtimeCheck realtimeCheck,MobileRealtimeStatus mobileRealtimeStatus) {
		if("1".equals(mobileRealtimeStatus.getNumberType())) {
			switch (MobileStatusStardardYdStateEnum.getEnum(Integer.valueOf(mobileRealtimeStatus.getStatus()))) {
			case NORMAL:
				if("1".equals(mobileRealtimeStatus.getMnpStatus())) {
					realtimeCheck.setNumberPortability(1L);
				}else {
					realtimeCheck.setNormal(1L);
				}
				break;
			case TINGJI:
			case QIANFEI:
				realtimeCheck.setServiceSuspended(1L);
				break;
			case ONLINE_NOTUSE:
				realtimeCheck.setOnlineButNotAvailable(1L);
				break;
			case NOT_AVAILABLE:
			case NO_MESSAGE:
			case NOT_EXISTS:
				realtimeCheck.setEmpty(1L);
				break;
			case GUANJI:
				realtimeCheck.setShutdown(1L);
				break;
			default:
				break;
			}
		}else if("2".equals(mobileRealtimeStatus.getNumberType())) {
			switch (MobileStatusStardardLdStateEnum.getEnum(Integer.valueOf(mobileRealtimeStatus.getStatus()))) {
			case NORMAL:
				if("1".equals(mobileRealtimeStatus.getMnpStatus())) {
					realtimeCheck.setNumberPortability(1L);
				}else {
					realtimeCheck.setNormal(1L);
				}
				break;
			case NOT_START:
			case NOT_EXISTS:
				realtimeCheck.setEmpty(1L);
				break;
			case QIANFEI:
			case TINGJI:
				realtimeCheck.setServiceSuspended(1L);
				break;
			case GUANJI:
				realtimeCheck.setShutdown(1L);
				break;
			default:
				break;
			}
		}else if("3".equals(mobileRealtimeStatus.getNumberType())) {
			switch (MobileStatusStardardDxStateEnum.getEnum(Integer.valueOf(mobileRealtimeStatus.getStatus()))) {
			case NORMAL:
				if("1".equals(mobileRealtimeStatus.getMnpStatus())) {
					realtimeCheck.setNumberPortability(1L);
				}else {
					realtimeCheck.setNormal(1L);
				}
				break;
			case TINGJI:
				realtimeCheck.setServiceSuspended(1L);
				break;
			case ONLINE_NOUSE:
				realtimeCheck.setOnlineButNotAvailable(1L);
				break;
			case NOT_EXISTS:
			case NOT_QUERY:
				realtimeCheck.setEmpty(1L);
				break;
			case GUANJI:
				realtimeCheck.setShutdown(1L);
				break;
			default:
				break;
			}
		}else {
			log.error("无此运营商类型");
		}
		
		return realtimeCheck;
	}
	
	private CustomerConsume getCustomerConsumeData(CustomerInfoVo customerInfoVo,RealtimeCheck realtimeCheck,Long balance,
			Integer consumeType,int chargeCount,Integer productCode) {
		CustomerConsume customerConsume = new CustomerConsume();
		customerConsume.setAgentId(customerInfoVo.getAgentId())
		.setId(snowflake.nextId())
        .setConsumeNumber(realtimeCheck.getTotalNumber())
        .setCustomerId(customerInfoVo.getCustomerId())
        .setName(customerInfoVo.getCustomerName())
        .setPhone(customerInfoVo.getPhone())
        .setVersion(0)
        .setCategory(productCode)
        .setConsumeType(consumeType)
        .setEmptyId(realtimeCheck.getId())
        .setOpeningBalance(balance + chargeCount)
        .setClosingBalance(balance);
		return customerConsume;
	}
}

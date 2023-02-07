package com.zhongzhi.empty.service.balance;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.entity.CustomerBalance;
import com.zhongzhi.empty.enums.ProductEnum;
import com.zhongzhi.empty.redis.DistributedLockWrapper;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.response.LuaByDeductFeeResponse;
import com.zhongzhi.empty.service.LuaExpressionService;
import com.zhongzhi.empty.service.customer.CustomerBalanceService;
import com.zhongzhi.empty.util.DingDingMessage;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPool;

/**
 * 余额实现类
 * @author liuh
 * @date 2021年10月27日
 */
@Slf4j
@Service
public class BalanceService {
	
	@Autowired
	private JedisPool jedisPool;
	
	@Autowired
	private RedisClient redisClient;
	
	@Autowired
	private LuaExpressionService luaExpressionService;
	
	@Autowired
	private CustomerBalanceService customerBalanceService;
	
	@Autowired
	private DingDingMessage dingDingMessage;

	public Boolean preDeductFee(Long customerId,Integer chargeCount,int productCode,Long settleId) {
		try {
			//获取用户余额
			Long balanceLong = getAtomicLongByCustomer(customerId, productCode);
			if(balanceLong == null) {
				return false;
			}
			
			String[] keys = {getBalanceKeyByProductCode(customerId, productCode),
					getFreezeAmountKeyByProductCode(customerId, productCode)};
			String[] params = {String.valueOf(chargeCount)};
			
			boolean result = luaExpressionService.luaByPreDeductFee(Arrays.asList(keys),Arrays.asList(params));
			log.info("{}, pre deduct fee {}，chargeCount：{}，productCode:{},settleId:{}",customerId,result?"success":"failed,balance is not enough",
					chargeCount,productCode,settleId);
			return result;
		} catch (Exception e) {
			log.error("{}, pre deduct fee exception，chargeCount：{}，productCode:{},settleId:{},info:",customerId,chargeCount,productCode,settleId,e);
			dingDingMessage.sendMessage(String.format("警告：%s, pre deduct fee exception，chargeCount：%s,productCode:%s,settleId:%s,info:%s", 
					customerId,chargeCount,productCode,settleId,e));
			return false;
		}
		
	}
	
	public Boolean backDeductFee(Long customerId,Integer chargeCount,int productCode,Long settleId) {
		try {
			String[] keys = {getBalanceKeyByProductCode(customerId, productCode),
					getFreezeAmountKeyByProductCode(customerId, productCode)};
			String[] params = {String.valueOf(chargeCount)};
			luaExpressionService.luaByBackDeductFee(Arrays.asList(keys),Arrays.asList(params));
			
			log.info("{}, back deduct fee success,chargeCount:{},productCode:{},settleId:{}",customerId,chargeCount,productCode,settleId);
			return true;
		} catch (Exception e) {
			log.error("{}, back deduct fee exception，chargeCount:{},productCode:{},settleId:{},info:",customerId,chargeCount,productCode,settleId,e);
			dingDingMessage.sendMessage(String.format("警告：%s, back deduct fee exception，chargeCount:%s,productCode:%s,settleId:%s,info:%s",
					customerId,chargeCount,productCode,settleId,e));
			return false;
		}
	}
	
	public LuaByDeductFeeResponse deductFee(Long customerId, Integer preChargeCount, Integer chargeCount,
			int productCode,Long settleId) {
		try {
			String[] keys = { getBalanceKeyByProductCode(customerId, productCode),
					getFreezeAmountKeyByProductCode(customerId, productCode),
					getRealDeductFeeKeyByProductCode(customerId, productCode)};
			long diffFee = preChargeCount - chargeCount;
			String[] params = { String.valueOf(diffFee), String.valueOf(preChargeCount),
					String.valueOf(chargeCount) };
			
			LuaByDeductFeeResponse result = luaExpressionService.luaByDeductFee(diffFee, Arrays.asList(keys), Arrays.asList(params));
			log.info("{},deduct fee {},preChargeCount:{},chargeCount:{},productCode:{},settleId:{}",customerId,result==null?"failed":"success",
					preChargeCount,chargeCount,productCode,settleId);
			return result;
		} catch (Exception e) {
			log.error("{}, deduct fee exception，preChargeCount:{},chargeCount:{},productCode:{},settleId:{},info:",
					customerId,preChargeCount,chargeCount,productCode,settleId,e);
			dingDingMessage.sendMessage(String.format("警告：%s, deduct fee exception，preChargeCount:%s,chargeCount:%s,productCode：%s,settleId:%s, info:%s", 
					customerId,preChargeCount,chargeCount,productCode,settleId,e));
			return null;
		}
	}
	
	public Object getCustomerRedisData(Long customerId) {
		String[] keys = {	getFreezeAmountKeyByProductCode(customerId, ProductEnum.EMPTY.getProductCode()),
							getRealDeductFeeKeyByProductCode(customerId, ProductEnum.EMPTY.getProductCode()),
							getBalanceKeyByProductCode(customerId, ProductEnum.EMPTY.getProductCode()),
							
							getFreezeAmountKeyByProductCode(customerId, ProductEnum.REALTIME.getProductCode()),
							getRealDeductFeeKeyByProductCode(customerId, ProductEnum.REALTIME.getProductCode()),
							getBalanceKeyByProductCode(customerId, ProductEnum.REALTIME.getProductCode()),
							
							getFreezeAmountKeyByProductCode(customerId, ProductEnum.INTERNATIONAL.getProductCode()),
							getRealDeductFeeKeyByProductCode(customerId, ProductEnum.INTERNATIONAL.getProductCode()),
							getBalanceKeyByProductCode(customerId, ProductEnum.INTERNATIONAL.getProductCode()),
							
							getFreezeAmountKeyByProductCode(customerId, ProductEnum.DIRECT_COMMON.getProductCode()),
							getRealDeductFeeKeyByProductCode(customerId, ProductEnum.DIRECT_COMMON.getProductCode()),
							getBalanceKeyByProductCode(customerId, ProductEnum.DIRECT_COMMON.getProductCode()),
							
							getFreezeAmountKeyByProductCode(customerId, ProductEnum.LINE_DIRECT.getProductCode()),
							getRealDeductFeeKeyByProductCode(customerId, ProductEnum.LINE_DIRECT.getProductCode()),
							getBalanceKeyByProductCode(customerId, ProductEnum.LINE_DIRECT.getProductCode())
						};
        return luaExpressionService.luaByGetCustomerRedisData(Arrays.asList(keys));
	}
	
	public void subRealDeductBalance(Long customerId,Long emptyRealCount,Long realtimeRealCount,Long internationalRealCount
							,Long directCommonRealCount,Long lineDirectRealCount) {
		String[] keys = {   getRealDeductFeeKeyByProductCode(customerId, ProductEnum.EMPTY.getProductCode()),
							getRealDeductFeeKeyByProductCode(customerId, ProductEnum.REALTIME.getProductCode()),
							getRealDeductFeeKeyByProductCode(customerId, ProductEnum.INTERNATIONAL.getProductCode()),
							getRealDeductFeeKeyByProductCode(customerId, ProductEnum.DIRECT_COMMON.getProductCode()),
							getRealDeductFeeKeyByProductCode(customerId, ProductEnum.LINE_DIRECT.getProductCode())
						};
		String[] params = {String.valueOf(emptyRealCount),String.valueOf(realtimeRealCount),String.valueOf(internationalRealCount)
							,String.valueOf(directCommonRealCount),String.valueOf(lineDirectRealCount)};
		luaExpressionService.subRealDeductBalance(Arrays.asList(keys),Arrays.asList(params));
	}
	
	/**
	 * 查询客户余额
	 * @param accNo
	 * @return
	 */
	public Long getAtomicLongByCustomer(Long customerId,int productCode) {
		String balanceStr = redisClient.get(getBalanceKeyByProductCode(customerId, productCode));
		if (StringUtils.isBlank(balanceStr)) {
			DistributedLockWrapper lock = new DistributedLockWrapper(jedisPool,getBalanceKeyByProductCode(customerId, productCode)+"lock",200L, 1000);
			try {
				if (StringUtils.isNotBlank(lock.getIdentifier())) {
					CustomerBalance customerBalance = customerBalanceService.findOneByCustomerId(customerId);
					log.info("{}, get customer balance ,balance:{}",customerId,JSON.toJSONString(customerBalance));
					if(customerBalance == null) {
						dingDingMessage.sendMessage(String.format("警告：%s, 查询客户余额失败，无余额记录", customerId));
						return null;
					}else {
						redisClient.set(getBalanceKeyByProductCode(customerId, productCode), getBalanceByProductCode(customerBalance, productCode).toString());
						return getBalanceByProductCode(customerBalance, productCode);
					}					
	            }else {
	            	try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						log.error("{}, get customer balance exception, info:",customerId,e);
					}
	            	
					return getAtomicLongByCustomer(customerId,productCode);
	            }				
			} catch (Exception e) {
				log.error("{}, get customer balance exception, info:",customerId,e);
				dingDingMessage.sendMessage(String.format("警告：%s, get customer balance exception, info:%s", customerId,e));
			}finally {
				lock.releaseLock();
			}
		}
		
		return Long.valueOf(balanceStr);
	}
	
	private String getBalanceKeyByProductCode(Long customerId,int productCode) {
		return ProductEnum.getProductBalanceKey(productCode, customerId);
	}
	
	private String getFreezeAmountKeyByProductCode(Long customerId,int productCode) {
		return ProductEnum.getProductFreezeKey(productCode, customerId);
	}
	
	private String getRealDeductFeeKeyByProductCode(Long customerId,int productCode) {
		return ProductEnum.getProductDeductKey(productCode, customerId);
	}
	
	private Long getBalanceByProductCode(CustomerBalance customerBalance,int productCode) {
		ProductEnum productEnum = ProductEnum.getProductEnum(productCode);
		switch (productEnum) {
		case EMPTY: return customerBalance.getEmptyCount(); 
		case REALTIME: return customerBalance.getRealtimeCount(); 
		case REALTIME_STARDARD: return customerBalance.getRealtimeCount(); 
		case INTERNATIONAL: return customerBalance.getInternationalCount(); 
		case DIRECT_COMMON: return customerBalance.getDirectCommonCount(); 
		case LINE_DIRECT: return customerBalance.getLineDirectCount(); 

		default:break;
		}
		
		return null;
	}
}

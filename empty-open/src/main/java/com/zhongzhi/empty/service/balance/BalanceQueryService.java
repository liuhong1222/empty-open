package com.zhongzhi.empty.service.balance;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.constants.RedisKeyConstant;
import com.zhongzhi.empty.entity.CustomerBalance;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.service.customer.CustomerBalanceService;
import com.zhongzhi.empty.util.ThreadLocalContainer;
import com.zhongzhi.empty.vo.CustomerInfoVo;

import lombok.extern.slf4j.Slf4j;

/**
 * 余额查询实现类
 * @author liuh
 * @date 2021年11月10日
 */
@Slf4j
@Service
public class BalanceQueryService {

	@Autowired
	private RedisClient redisClient;
	
	@Autowired
	private CustomerBalanceService customerBalanceService;
	
	public ApiResult balanceQuery(String ip) {
		CustomerBalance customerBalance = new CustomerBalance();
		// 线程缓存里获取用户信息
		CustomerInfoVo customerInfoVo = ThreadLocalContainer.getCustomerInfoVo();
		// 查询用户redis余额
		String emptyRedisBalance = redisClient.get(RedisKeyConstant.EMPTY_BALANCE_KEY+customerInfoVo.getCustomerId());
		String realtimeBalance = redisClient.get(RedisKeyConstant.REALTIME_BALANCE_KEY+customerInfoVo.getCustomerId());
		// 一个产品的redis余额为空则查询数据库余额
		if(StringUtils.isBlank(emptyRedisBalance) || StringUtils.isBlank(realtimeBalance)) {
			customerBalance = customerBalanceService.findOneByCustomerId(customerInfoVo.getCustomerId());
		}
		
		if(customerBalance == null) {
			return ApiResult.fail("该账户余额记录不存在");
		}
		
		Map<String, String> map = new HashMap<>();
        map.put("emptyRemain", StringUtils.isBlank(emptyRedisBalance)?String.valueOf(customerBalance.getEmptyCount()):emptyRedisBalance + "条");
        map.put("realtimeRemain", StringUtils.isBlank(realtimeBalance)?String.valueOf(customerBalance.getRealtimeCount()):realtimeBalance + "条");
        
        log.info("{}, 查询余额成功，info:{}",customerInfoVo.getCustomerId(),JSON.toJSONString(map));
		return ApiResult.ok(map);
	}
}

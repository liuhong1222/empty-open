package com.zhongzhi.empty.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.entity.Customer;
import com.zhongzhi.empty.entity.CustomerBalance;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.service.customer.CustomerBalanceService;
import com.zhongzhi.empty.service.customer.CustomerService;
import com.zhongzhi.empty.util.DingDingMessage;
import com.zhongzhi.empty.util.HttpUtils;
import com.zhongzhi.empty.util.ListUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 初始化数据实现类
 * @author liuh
 * @date 2021年11月10日
 */
@Slf4j
@Service
public class InitDataService {
	
	@Value("${old.system.balance.url}")
	private String oldSystemUrl;
	
	@Autowired
	private CustomerBalanceService customerBalanceService;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private DingDingMessage dingDingMessage;
	
	private final static Integer BATCH_NUM = 1000;
	
	public ApiResult customerBalanceHandle(Long customerId) {
		if(customerId != null) {
			return handleSingle(customerId);
		}
		
		long st = System.currentTimeMillis();
		int totalCount = 0;
		// 查询所有客户信息
		List<Customer> customerList = customerService.findAll();
		if(CollectionUtils.isEmpty(customerList)) {
			log.error("初始化全部用户余额数据失败，customer表为空");
			return ApiResult.fail("customer表为空");
		}
		
		List<List<Customer>> list = ListUtils.splitList(customerList, BATCH_NUM);
		for(List<Customer> tempList : list) {
			List<CustomerBalance> balanceList = new ArrayList<CustomerBalance>();
			for(Customer  customer : tempList) {
				CustomerBalance customerBalance = invokeOldSystem(customer.getId());
				if(customerBalance == null) {
					continue;
				}
				
				balanceList.add(customerBalance);
			}
			
			if(!CollectionUtils.isEmpty(balanceList)) {
				int counts = customerBalanceService.saveList(balanceList);
				if(counts < balanceList.size()) {
					log.error("初始化批次用户余额失败，数据批量入库失败，批次总数：{}，有效客户余额数量：{}，问题客户余额数量：{}，插入数量：{},param:{},info:{}",BATCH_NUM,balanceList.size(),
							BATCH_NUM-balanceList.size(),counts,JSON.toJSONString(tempList),JSON.toJSONString(balanceList));
					dingDingMessage.sendMessage(String.format("初始化批次用户余额失败，数据批量入库失败，批次总数：%s，有效客户余额数量：%s，问题客户余额数量：%s，插入数量：%s",BATCH_NUM,balanceList.size(),
							BATCH_NUM-balanceList.size(),counts));
					continue;
				}
				
				log.info("初始化批次用户余额成功，批次总数：{}，有效客户余额数量：{}，问题客户余额数量：{}",BATCH_NUM,balanceList.size(),
						BATCH_NUM-balanceList.size());
				totalCount += balanceList.size();
			}
		}
		
		log.info("初始化全部用户余额成功，客户总数：{}，有效客户余额数量：{}，问题客户余额数量：{}, useTime:{}",
							customerList.size(),totalCount,customerList.size()-totalCount,System.currentTimeMillis()-st);
		return ApiResult.ok();
	}
	
	private ApiResult handleSingle(Long customerId) {
		CustomerBalance customerBalance = invokeOldSystem(customerId);
		if(customerBalance == null) {
			return ApiResult.fail("老系统余额接口查询失败");
		}
		
		int counts = customerBalanceService.saveOne(customerBalance);
		if(counts < 1) {
			log.error("{}, 调整单个用户余额失败，数据库入库失败，info:{}",customerId,JSON.toJSONString(customerBalance));
			return ApiResult.fail("数据库入库失败");
		}
		
		log.info("{}, 初始化单个用户余额成功，info:{}",customerId,JSON.toJSONString(customerBalance));
		return ApiResult.ok();
	}
	
	private CustomerBalance invokeOldSystem(Long customerId) {
		CustomerBalance customerBalance = new CustomerBalance();
		customerBalance.setCustomerId(customerId);
		
		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("id", customerId.toString());
		String response = HttpUtils.get(oldSystemUrl, paramsMap);
		if(StringUtils.isBlank(response)) {
			log.error("{}, 查询老系统余额失败，老系统接口返回为空", customerId);
			dingDingMessage.sendMessage(String.format("警告：%s,查询老系统余额失败，老系统接口返回为空", customerId));
			return null;
		}
		
		JSONObject json = JSONObject.parseObject(response);
		if(!CommonConstant.OLD_SYSTEM_SUCCESS_CODE.equals(json.get("code").toString())) {
			log.error("{}, 查询老系统余额失败，response:{}", customerId,response);
			dingDingMessage.sendMessage(String.format("警告：%s,查询老系统余额失败，msg:%s", customerId,json.getString("msg")));
			return null;
		}
		
		log.info("{}, 查询老系统余额成功，response:{}",customerId,response);
		JSONObject dataJson = JSONObject.parseObject(json.getString("data"));
		customerBalance.setEmptyCount(Long.valueOf(dataJson.getString("emptyRemain").replace("条", "")));
		customerBalance.setRealtimeCount(Long.valueOf(dataJson.getString("realtimeRemain").replace("条", "")));
		return customerBalance;
	}
}

package com.zhongzhi.empty.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.entity.Agent;
import com.zhongzhi.empty.entity.ApiSettings;
import com.zhongzhi.empty.entity.Customer;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.service.customer.ApiSettingsService;
import com.zhongzhi.empty.service.customer.CustomerService;
import com.zhongzhi.empty.vo.CustomerInfoVo;

import lombok.extern.slf4j.Slf4j;

/**
 * Api签名及账号校验实现类
 * @author liuh
 * @date 2021年10月26日
 */
@Slf4j
@Service
public class ApiSignService {
	
	@Autowired
	private ApiSettingsService apiSettingsService;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private AgentService agentService;
	
	public ApiResult<CustomerInfoVo> getCustomerInfoVo(String appId, String appKey){
		try {
			//查询api账号信息
			ApiSettings apiSettings = apiSettingsService.getByAppId(appId);
	        if (apiSettings == null) {
	            log.error("此接口账号不存在，appId：{}", appId);
	            return ApiResult.result(ApiCode.UNAUTHORIZED, "帐号不存在", null);
	        }
	        if (apiSettings.getState() == 0) {
	            log.error("此接口账号已禁用，appId：{}", appId);
	            return ApiResult.result(ApiCode.UNAUTHENTICATED_EXCEPTION, "帐号已禁用", null);
	        }
	        if (!apiSettings.getAppKey().equalsIgnoreCase(appKey)) {
	            log.error("接口账号密码不正确，appId: {}, 系统保存密码：{}, 接口传入密码：{}", appId, apiSettings.getAppKey(), appKey);
	            return ApiResult.result(ApiCode.PARAMETER_EXCEPTION, "帐号或密码错误", null);
	        }
	        
	        //查询用户信息
	        Customer customer = customerService.getCustomerById(apiSettings.getCustomerId());
	        if (customer == null) {
	            log.error("客户信息不存在，appId: {}, customerId: {}", apiSettings.getAppId(), apiSettings.getCustomerId());
	            return ApiResult.result(ApiCode.UNAUTHORIZED, "账号信息不存在", null);
	        }
	        if (customer.getState() != 9) {
	            log.error("客户账号未实名认证，appId: {}, customerId: {}", apiSettings.getAppId(), customer.getId());
	            return ApiResult.result(ApiCode.NOT_PERMISSION, "账号未实名认证", null);
	        }

	        Agent agent = agentService.getAgentById(customer.getAgentId());
	        if (agent == null) {
	            log.error("代理商信息不存在，appId：{}, agentId: {}", apiSettings.getAppId(), customer.getAgentId());
	            return ApiResult.result(ApiCode.UNAUTHORIZED, "资源账号不存在", null);
	        }
	        if (agent.getState() != 1) {
	            log.error("代理商已禁用，appId: {}, agentId: {}", apiSettings.getAppId(), agent.getId());
	            return ApiResult.result(ApiCode.NOT_PERMISSION, "资源账号已被禁用", null);
	        }

	        CustomerInfoVo customerInfoVo = new CustomerInfoVo();
	        customerInfoVo.setAgentId(agent.getId());
	        customerInfoVo.setCompanyName(agent.getCompanyName());
	        customerInfoVo.setCustomerId(customer.getId());
	        customerInfoVo.setCustomerName(customer.getName());
	        customerInfoVo.setPhone(customer.getPhone());
	        return ApiResult.ok(customerInfoVo);
		} catch (Exception e) {
			log.error("{}，接口调用异常，info:",appId,e);
			return ApiResult.fail(ApiCode.SYSTEM_EXCEPTION);
		}
	}
}

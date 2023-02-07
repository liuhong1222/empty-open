package com.zhongzhi.empty.service.customer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.dao.CustomerBalanceMapper;
import com.zhongzhi.empty.entity.CustomerBalance;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户余额实现类
 * @author liuh
 * @date 2021年10月28日
 */
@Slf4j
@Service
public class CustomerBalanceService {

	@Autowired
	private CustomerBalanceMapper customerBalanceMapper;
	
	public CustomerBalance findOneByCustomerId(Long customerId) {
		return customerBalanceMapper.findOneByCustomerId(customerId);
	}
	
	public CustomerBalance findOneByCustomerIdForUpdate(Long customerId) {
		return customerBalanceMapper.findOneByCustomerIdForUpdate(customerId);
	}
	
	public int balanceSettlement(Long customerId,Long emptyRealCount,Long realtimeRealCount,Long internationalCount,
			Long directCommonRealCount,Long lineDirectRealCount,Long lastTime) {
		return customerBalanceMapper.balanceSettlement(customerId, emptyRealCount, realtimeRealCount,internationalCount, 
				directCommonRealCount,lineDirectRealCount,lastTime);
	}
	
	public int saveOne(CustomerBalance customerBalance) {
		return customerBalanceMapper.saveOne(customerBalance);
	}
	
	public int saveList(List<CustomerBalance> list) {
		return customerBalanceMapper.saveList(list);
	}
}

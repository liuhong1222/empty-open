package com.zhongzhi.empty.service.customer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.constants.CaffeineConstant;
import com.zhongzhi.empty.dao.CustomerMapper;
import com.zhongzhi.empty.entity.Customer;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户信息实现类
 * @author liuh
 * @date 2021年10月26日
 */
@Slf4j
@Service
public class CustomerService {

	@Autowired
    private CustomerMapper customerMapper;
	
	@Cacheable(cacheManager = "caffeineCacheManager", value = CaffeineConstant.CUSTOMER_INFO, key = "#id", unless = "#result == null")
	public Customer getCustomerById(Long id) {
        return customerMapper.getCustomerById(id);
    }
	
	@CacheEvict(cacheManager = "caffeineCacheManager", cacheNames = {CaffeineConstant.CUSTOMER_INFO}, key = "#id")
	public void refreshGetCustomerById(Long id) {
		log.info("{}，用户信息缓存刷新成功",id);
	}
	
	public List<Customer> findAll(){
		return customerMapper.findAll();
	}
}

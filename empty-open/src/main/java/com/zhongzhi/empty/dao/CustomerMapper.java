package com.zhongzhi.empty.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.Customer;

@Mapper
public interface CustomerMapper {

	/**
	 * 获取用户信息
	 * @param id
	 * @return
	 */
	Customer getCustomerById(@Param("id") Long id);
	
	List<Customer> findAll();
}

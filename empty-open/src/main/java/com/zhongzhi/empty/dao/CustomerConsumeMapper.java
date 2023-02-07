package com.zhongzhi.empty.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.zhongzhi.empty.entity.CustomerConsume;

@Mapper
public interface CustomerConsumeMapper {

	int saveList(List<CustomerConsume> list);
	
	int updateOne(CustomerConsume customerConsume);
}

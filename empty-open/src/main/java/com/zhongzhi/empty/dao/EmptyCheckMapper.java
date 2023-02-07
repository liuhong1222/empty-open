package com.zhongzhi.empty.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.EmptyCheck;

@Mapper
public interface EmptyCheckMapper {

	int saveList(List<EmptyCheck> list);
	
	int saveOne(EmptyCheck emptyCheck);
	
	int updateOne(EmptyCheck emptyCheck);
	
	EmptyCheck findOne(@Param("customerId")Long customerId,@Param("emptyId")Long emptyId);
}

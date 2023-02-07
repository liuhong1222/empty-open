package com.zhongzhi.empty.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.InternationalCheck;

import java.util.List;

@Mapper
public interface InternationalCheckMapper {

	int saveList(List<InternationalCheck> list);
	
	int saveOne(InternationalCheck internationalCheck);
	
	int updateOne(InternationalCheck internationalCheck);
	
	InternationalCheck findOne(@Param("customerId")Long customerId,@Param("internationalId")Long internationalId);

	int delete(Long id);
}

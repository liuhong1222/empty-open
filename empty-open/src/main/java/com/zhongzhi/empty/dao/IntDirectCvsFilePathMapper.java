package com.zhongzhi.empty.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.IntDirectCvsFilePath;

@Mapper
public interface IntDirectCvsFilePathMapper {

	int saveOne(IntDirectCvsFilePath intDirectCvsFilePath);
	
	IntDirectCvsFilePath findOne(@Param("customerId")Long customerId, @Param("intDirectId")Long intDirectId);

    int delete(Long id);
}

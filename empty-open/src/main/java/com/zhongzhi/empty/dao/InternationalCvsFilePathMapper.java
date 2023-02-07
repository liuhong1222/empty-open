package com.zhongzhi.empty.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.InternationalCvsFilePath;

@Mapper
public interface InternationalCvsFilePathMapper {

	int saveOne(InternationalCvsFilePath internationalCvsFilePath);
	
	InternationalCvsFilePath findOne(@Param("customerId")Long customerId, @Param("internationalId")Long internationalId);

    int delete(Long id);
}

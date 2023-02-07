package com.zhongzhi.empty.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.CvsFilePath;

@Mapper
public interface CvsFilePathMapper {
	
	int saveOne(CvsFilePath cvsFilePath);
	
	CvsFilePath findOne(@Param("customerId")Long customerId,@Param("emptyId")Long emptyId);
}

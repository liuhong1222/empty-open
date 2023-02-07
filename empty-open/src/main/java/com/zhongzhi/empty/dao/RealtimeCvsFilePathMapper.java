package com.zhongzhi.empty.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.RealtimeCvsFilePath;

@Mapper
public interface RealtimeCvsFilePathMapper {
	
	int saveOne(RealtimeCvsFilePath realtimeCvsFilePath);
	
	RealtimeCvsFilePath findOne(@Param("customerId")Long customerId,@Param("realtimeId")Long realtimeId);
}

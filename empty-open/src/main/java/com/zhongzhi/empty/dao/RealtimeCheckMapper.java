package com.zhongzhi.empty.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.RealtimeCheck;

@Mapper
public interface RealtimeCheckMapper {

	int saveList(List<RealtimeCheck> list);
	
	int saveOne(RealtimeCheck realtimeCheck);
	
	int updateOne(RealtimeCheck realtimeCheck);
	
	RealtimeCheck findOne(@Param("customerId")Long customerId,@Param("emptyId")Long emptyId);
}

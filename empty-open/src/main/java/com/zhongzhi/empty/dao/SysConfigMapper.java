package com.zhongzhi.empty.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.SysConfig;

@Mapper
public interface SysConfigMapper {

	SysConfig findOneByKey(@Param("keys") String keys);
}

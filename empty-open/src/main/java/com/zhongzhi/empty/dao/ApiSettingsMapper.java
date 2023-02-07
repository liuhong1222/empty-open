package com.zhongzhi.empty.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.ApiSettings;

@Mapper
public interface ApiSettingsMapper {

	/**
	 * 查询用户api账号信息
	 * @param appId
	 * @return
	 */
	ApiSettings getByAppId(@Param("appId") String appId);
}

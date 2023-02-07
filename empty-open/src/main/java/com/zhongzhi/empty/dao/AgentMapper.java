package com.zhongzhi.empty.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.Agent;

@Mapper
public interface AgentMapper {
	
	/**
	 * 获取代理商信息
	 * @param id
	 * @return
	 */
	Agent getAgentById(@Param("id") Long id);
}

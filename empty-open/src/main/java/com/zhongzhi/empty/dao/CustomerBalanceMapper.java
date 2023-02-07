package com.zhongzhi.empty.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.CustomerBalance;

@Mapper
public interface CustomerBalanceMapper {

	/**
	 * 获取用户余额信息
	 * @param id
	 * @return
	 */
	CustomerBalance findOneByCustomerId(@Param("customerId") Long customerId);
	
	/**
	 * 获取用户余额信息-结算
	 * @param id
	 * @return
	 */
	CustomerBalance findOneByCustomerIdForUpdate(@Param("customerId") Long customerId);
	
	/**
	 * 余额结算
	 * @param customerId
	 * @param emptyRealCount
	 * @param realtimeRealCount
	 * @param lastTime
	 * @return
	 */
	int balanceSettlement(@Param("customerId") Long customerId,@Param("emptyRealCount") Long emptyRealCount,
			@Param("realtimeRealCount") Long realtimeRealCount,@Param("internationalCount") Long internationalCount,
			@Param("directCommonRealCount") Long directCommonRealCount,@Param("lineDirectRealCount") Long lineDirectRealCount,
			@Param("lastTime") Long lastTime);
	
	/**
	 * 单条插入
	 * @param customerBalance
	 * @return
	 */
	int saveOne(CustomerBalance customerBalance);
	
	/**
	 * 批量插入
	 * @param list
	 * @return
	 */
	int saveList(List<CustomerBalance> list);
}

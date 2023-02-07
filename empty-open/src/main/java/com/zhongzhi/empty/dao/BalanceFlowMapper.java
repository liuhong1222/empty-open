package com.zhongzhi.empty.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import com.zhongzhi.empty.entity.BalanceFlow;

@Mapper
public interface BalanceFlowMapper {
	
	int saveOne(BalanceFlow balanceFlow);	
	
	int saveList(List<BalanceFlow> list);
}

package com.zhongzhi.empty.service.balance;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.dao.BalanceFlowMapper;
import com.zhongzhi.empty.entity.BalanceFlow;

import lombok.extern.slf4j.Slf4j;

/**
 * 余额变更流水记录实现类
 * @author liuh
 * @date 2021年11月9日
 */
@Slf4j
@Service
public class BalanceFlowService {

	@Autowired
	private BalanceFlowMapper balanceFlowMapper;
	
	public int saveOne(BalanceFlow balanceFlow) {
		return balanceFlowMapper.saveOne(balanceFlow);
	}
	
	public int saveList(List<BalanceFlow> list) {
		return balanceFlowMapper.saveList(list);
	}
}

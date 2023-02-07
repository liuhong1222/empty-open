package com.zhongzhi.empty.service.customer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.dao.CustomerConsumeMapper;
import com.zhongzhi.empty.entity.CustomerConsume;
import com.zhongzhi.empty.util.DingDingMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * 消耗记录实现类
 * @author liuh
 * @date 2021年10月28日
 */
@Slf4j
@Service
public class CustomerConsumeService {

	@Autowired
	private CustomerConsumeMapper customerConsumeMapper;
	
	@Autowired
	private DingDingMessage dingDingMessage;
	
	private static ExecutorService executorService = Executors.newFixedThreadPool(10);
	
	public int updateOne(CustomerConsume customerConsume) {
		return customerConsumeMapper.updateOne(customerConsume);
	}
	
	public void saveList(List<CustomerConsume> list) {
		executorService.execute(new Runnable() {

			@Override
			public void run() {
				if(CollectionUtils.isEmpty(list)) {
					return ;
				}
				
				try {
					int counts = customerConsumeMapper.saveList(list);
					if (counts != list.size()) {
						log.error("批量插入消耗记录失败，count:{},list:{}", list.size(), JSON.toJSONString(list));
						dingDingMessage.sendMessage(String.format("警告：批量插入消耗记录失败，count:%s", list.size()));
						return;
					}

					log.info("批量插入消耗记录成功，count:{}", list.size());
				} catch (Exception e) {
					log.error("批量插入消耗记录异常，count:{},list:{}, info:", list.size(), JSON.toJSONString(list), e);
					dingDingMessage.sendMessage(String.format("警告：批量插入消耗记录异常，count:%s", list.size()));
				}
			}
		});
	}
}

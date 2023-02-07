package com.zhongzhi.empty.service.realtime;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.dao.RealtimeCheckMapper;
import com.zhongzhi.empty.entity.RealtimeCheck;
import com.zhongzhi.empty.util.DingDingMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * 实时检测记录实现类
 * @author liuh
 * @date 2021年11月2日
 */
@Slf4j
@Service
public class RealtimeCheckService {

	@Autowired
	private RealtimeCheckMapper realtimeCheckMapper;
	
	@Autowired
	private DingDingMessage dingDingMessage;
	
	private static ExecutorService executorService = Executors.newFixedThreadPool(10);
	
	public RealtimeCheck findOne(Long customerId,Long emptyId) {
		return realtimeCheckMapper.findOne(customerId, emptyId);
	}
	
	public int updateOne(RealtimeCheck realtimeCheck) {
		return realtimeCheckMapper.updateOne(realtimeCheck);
	}
	
	public int saveOne(RealtimeCheck realtimeCheck) {
		return realtimeCheckMapper.saveOne(realtimeCheck);
	}
	
	public void saveList(List<RealtimeCheck> list) {
		executorService.execute(new Runnable() {
			
			@Override
			public void run() {
				if(CollectionUtils.isEmpty(list)) {
					return ;
				}
				
				try {
					int counts = realtimeCheckMapper.saveList(list);
					if(counts != list.size()) {
						log.error("批量插入实时检测记录失败，count:{},list:{}",list.size(),JSON.toJSONString(list));
						dingDingMessage.sendMessage(String.format("警告：批量插入实时检测记录失败，count:%s", list.size()));
						return ;
					}
					
					log.info("批量插入实时检测记录成功，count:{}",list.size());
				} catch (Exception e) {
					log.error("批量插入实时检测记录异常，count:{},list:{}, info:",list.size(),JSON.toJSONString(list),e);
					dingDingMessage.sendMessage(String.format("警告：批量插入实时检测记录异常，count:%s", list.size()));
				}
			}
		});
	}
}

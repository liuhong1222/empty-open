package com.zhongzhi.empty.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import com.zhongzhi.empty.entity.CustomerConsume;
import com.zhongzhi.empty.service.customer.CustomerConsumeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 检测记录批量入库实现
 * @author liuh
 * @date 2021年10月28日
 */
@Component
@Slf4j
public class CustomerConsumeDataTask extends Thread {
	
	// 本地缓存，批量入库
	private static Map<String,CustomerConsume> dataCache = new ConcurrentHashMap<String,CustomerConsume>();;
	// 不够1000条则10秒钟入库一次
	private static final int INSERT_INTERVAL = 10 * 1000;
	
	@Value("${customer.consume.nums.limit}")
	private int MAX_CACHE_SIZE;
	
	private static CustomerConsumeLocalCache localCache = CustomerConsumeLocalCache.getInStance();
	
	private boolean alive = true;
	
	@Autowired
	private CustomerConsumeService customerConsumeService;
	
	@PostConstruct
	public void init() {
		this.start();
		log.info("CustomerConsumeDataTask start ......");
	}
	
	public void kill() {
		if(localCache.getCacheSize() == 0){
			this.alive = false;
			this.interrupt();
			log.info("CustomerConsumeDataTask end ......");
		}
	}
	
	@Override
	public void run() {
		long lastInsertTime = System.currentTimeMillis();
		while(alive) {
			try {
				CustomerConsume data = localCache.getLocalCache();
				if(data != null) {
					dataCache.put(data.getId().toString(),data);
				}else {
					Thread.sleep(10L);
				}
				
				if(!alive){
					batchPushData();
				}else{
					if (dataCache.size() >= MAX_CACHE_SIZE
							|| (System.currentTimeMillis() - lastInsertTime) > INSERT_INTERVAL) {
						batchPushData();
						dataCache.clear();
						lastInsertTime = System.currentTimeMillis();
					}
				}

			} catch (Exception e) {
				log.error("CustomerConsumeDataTask batchPushData fail: " + dataCache.size(),e);
			}
		}
	}
	
	/**
	 * 批量推送
	 */
	private  void batchPushData() {
		//dataCache获取所有的value
		List<CustomerConsume> totalLst = new ArrayList<CustomerConsume>(dataCache.values());
		if(CollectionUtils.isEmpty(totalLst)) {
			return;
		}
		
		log.info("====batch insert customer_consume  data====data size:{}", totalLst.size());
		customerConsumeService.saveList(totalLst);
	}
}

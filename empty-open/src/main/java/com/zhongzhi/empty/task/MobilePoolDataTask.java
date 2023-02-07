package com.zhongzhi.empty.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import com.zhongzhi.empty.entity.MobileStatusCache;
import com.zhongzhi.empty.service.LocalCacheService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 号码池批量入库实现
 * @author liuh
 * @date 2021年10月28日
 */
@Component
@Slf4j
public class MobilePoolDataTask extends Thread {
	
	// 本地缓存，批量入库
	private static Map<String,List<MobileStatusCache>> dataCache = new ConcurrentHashMap<String,List<MobileStatusCache>>();
	// 不够1000条则10秒钟入库一次
	private static final int INSERT_INTERVAL = 10 * 1000;
	
	@Value("${mobile.pool.nums.limit}")
	private int MAX_CACHE_SIZE;
	
	private static MobilePoolLocalCache localCache = MobilePoolLocalCache.getInStance();
	
	private boolean alive = true;
	
	@Autowired
	private LocalCacheService localCacheService;
	
	@PostConstruct
	public void init() {
		this.start();
		log.info("MobilePoolDataTask start ......");
	}
	
	public void kill() {
		if(localCache.getCacheSize() == 0){
			this.alive = false;
			this.interrupt();
			log.info("MobilePoolDataTask end ......");
		}
	}
	
	@Override
	public void run() {
		long lastInsertTime = System.currentTimeMillis();
		while(alive) {
			try {
				List<MobileStatusCache> data = localCache.getLocalCache();
				if(!CollectionUtils.isEmpty(data)) {
					dataCache.put(UUID.randomUUID().toString(),data);
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
				log.error("MobilePoolDataTask batchPushData fail: " + dataCache.size(),e);
			}
		}
	}
	
	/**
	 * 批量推送
	 */
	private  void batchPushData() {
		//dataCache获取所有的value
		List<List<MobileStatusCache>> list =  new ArrayList<List<MobileStatusCache>>(dataCache.values());
		if(CollectionUtils.isEmpty(list)) {
			return;
		}
		
		Set<MobileStatusCache> tempList = new HashSet<MobileStatusCache>();
		list.forEach(m -> {
			tempList.addAll(m);
		});		
		
		log.info("====batch insert mobile pool data====data size:{}", tempList.size());
		localCacheService.saveList(new ArrayList<MobileStatusCache>(tempList));
	}
}

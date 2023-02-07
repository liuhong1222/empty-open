package com.zhongzhi.empty.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import com.zhongzhi.empty.entity.RealtimeCheck;
import com.zhongzhi.empty.service.realtime.RealtimeCheckService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 实时检测记录批量入库实现
 * @author liuh
 * @date 2021年11月2日
 */
@Component
@Slf4j
public class RealtimeCheckDataTask extends Thread {
	
	// 本地缓存，批量入库
	private static Map<String,RealtimeCheck> dataCache = new ConcurrentHashMap<String,RealtimeCheck>();
	// 不够1000条则10秒钟入库一次
	private static final int INSERT_INTERVAL = 30 * 1000;
	
	@Value("${realtime.check.nums.limit}")
	private int MAX_CACHE_SIZE;
	
	private static RealtimeCheckLocalCache localCache = RealtimeCheckLocalCache.getInStance();
	
	private boolean alive = true;
	
	@Autowired
	private RealtimeCheckService realtimeCheckService;
	
	@PostConstruct
	public void init() {
		this.start();
		log.info("RealtimeCheckDataTask start ......");
	}
	
	public void kill() {
		if(localCache.getCacheSize() == 0){
			this.alive = false;
			this.interrupt();
			log.info("RealtimeCheckDataTask end ......");
		}
	}
	
	@Override
	public void run() {
		long lastInsertTime = System.currentTimeMillis();
		while(alive) {
			try {
				RealtimeCheck data = localCache.getLocalCache();
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
				log.error("RealtimeCheckDataTask batchPushData fail: " + dataCache.size(),e);
			}
		}
	}
	
	/**
	 * 批量推送
	 */
	private  void batchPushData() {
		//dataCache获取所有的value
		List<RealtimeCheck> totalLst = new ArrayList<RealtimeCheck>(dataCache.values());
		if(CollectionUtils.isEmpty(totalLst)) {
			return;
		}
		
		log.info("====batch insert realtime_check  data====data size:{}", totalLst.size());
		realtimeCheckService.saveList(totalLst);
	}
}

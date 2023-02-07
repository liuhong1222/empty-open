package com.zhongzhi.empty.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import com.zhongzhi.empty.entity.InternationalCheck;
import com.zhongzhi.empty.service.international.InternationalCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 国际检测记录批量入库实现
 * @author liuh
 * @date 2022年6月9日
 */
@Component
@Slf4j
public class InternationalCheckDataTask extends Thread {
	
	// 本地缓存，批量入库
	private static Map<String,InternationalCheck> dataCache = new ConcurrentHashMap<String,InternationalCheck>();
	// 不够1000条则10秒钟入库一次
	private static final int INSERT_INTERVAL = 10 * 1000;
	
	@Value("${international.check.nums.limit}")
	private int MAX_CACHE_SIZE;
	
	private static InternationalCheckLocalCache localCache = InternationalCheckLocalCache.getInStance();
	
	private boolean alive = true;
	
	@Autowired
	private InternationalCheckService internationalCheckService;
	
	@PostConstruct
	public void init() {
		this.start();
		log.info("InternationalCheckDataTask start ......");
	}
	
	public void kill() {
		if(localCache.getCacheSize() == 0){
			this.alive = false;
			this.interrupt();
			log.info("InternationalCheckDataTask end ......");
		}
	}
	
	@Override
	public void run() {
		long lastInsertTime = System.currentTimeMillis();
		while(alive) {
			try {
				InternationalCheck data = localCache.getLocalCache();
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
				log.error("InternationalCheckDataTask batchPushData fail: " + dataCache.size(),e);
			}
		}
	}
	
	/**
	 * 批量推送
	 */
	private  void batchPushData() {
		//dataCache获取所有的value
		List<InternationalCheck> totalLst = new ArrayList<InternationalCheck>(dataCache.values());
		if(CollectionUtils.isEmpty(totalLst)) {
			return;
		}
		
		log.info("====batch insert international_check  data====data size:{}", totalLst.size());
		internationalCheckService.saveList(totalLst);
	}
}

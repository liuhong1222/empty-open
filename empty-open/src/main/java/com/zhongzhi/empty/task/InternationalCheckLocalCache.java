package com.zhongzhi.empty.task;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.zhongzhi.empty.entity.InternationalCheck;

/**
 * 国际检测记录本地缓存
 * @author liuh
 * @date 2022年6月9日
 */
public class InternationalCheckLocalCache {
	
	// 缓存ConcurrentLinkedQueue
	private static ConcurrentLinkedQueue<InternationalCheck> dataCache = new ConcurrentLinkedQueue<InternationalCheck>();
	private static InternationalCheckLocalCache localCache = new InternationalCheckLocalCache();
	
	private InternationalCheckLocalCache() {
		
	}
	
	public InternationalCheck getLocalCache() {
		InternationalCheck value = dataCache.poll();
		return value;
	}
	
	public void setLocalCache(InternationalCheck value) {
		dataCache.add(value);
	}
	
	public static InternationalCheckLocalCache getInStance() {
		return localCache;
	}
	
	public int getCacheSize() {
		return dataCache.size();
	}
	
}

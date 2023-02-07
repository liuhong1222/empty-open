package com.zhongzhi.empty.task;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.zhongzhi.empty.entity.RealtimeCheck;

/**
 * 实时检测记录本地缓存
 * @author liuh
 * @date 2021年11月2日
 */
public class RealtimeCheckLocalCache {
	
	// 缓存ConcurrentLinkedQueue
	private static ConcurrentLinkedQueue<RealtimeCheck> dataCache = new ConcurrentLinkedQueue<RealtimeCheck>();
	private static RealtimeCheckLocalCache localCache = new RealtimeCheckLocalCache();
	
	private RealtimeCheckLocalCache() {
		
	}
	
	public RealtimeCheck getLocalCache() {
		RealtimeCheck value = dataCache.poll();
		return value;
	}
	
	public void setLocalCache(RealtimeCheck value) {
		dataCache.add(value);
	}
	
	public static RealtimeCheckLocalCache getInStance() {
		return localCache;
	}
	
	public int getCacheSize() {
		return dataCache.size();
	}
	
}

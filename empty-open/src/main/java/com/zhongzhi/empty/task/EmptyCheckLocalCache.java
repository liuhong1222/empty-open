package com.zhongzhi.empty.task;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.zhongzhi.empty.entity.EmptyCheck;

/**
 * 检测记录本地缓存
 * @author liuh
 * @date 2021年10月28日
 */
public class EmptyCheckLocalCache {
	
	// 缓存ConcurrentLinkedQueue
	private static ConcurrentLinkedQueue<EmptyCheck> dataCache = new ConcurrentLinkedQueue<EmptyCheck>();
	private static EmptyCheckLocalCache localCache = new EmptyCheckLocalCache();
	
	private EmptyCheckLocalCache() {
		
	}
	
	public EmptyCheck getLocalCache() {
		EmptyCheck value = dataCache.poll();
		return value;
	}
	
	public void setLocalCache(EmptyCheck value) {
		dataCache.add(value);
	}
	
	public static EmptyCheckLocalCache getInStance() {
		return localCache;
	}
	
	public int getCacheSize() {
		return dataCache.size();
	}
	
}

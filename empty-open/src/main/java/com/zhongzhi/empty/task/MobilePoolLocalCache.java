package com.zhongzhi.empty.task;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.zhongzhi.empty.entity.MobileStatusCache;

/**
 * 号码池本地缓存
 * @author liuh
 * @date 2021年10月28日
 */
public class MobilePoolLocalCache {
	
	// 缓存ConcurrentLinkedQueue
	private static ConcurrentLinkedQueue<List<MobileStatusCache>> dataCache = new ConcurrentLinkedQueue<List<MobileStatusCache>>();
	private static MobilePoolLocalCache localCache = new MobilePoolLocalCache();
	
	private MobilePoolLocalCache() {
		
	}
	
	public List<MobileStatusCache> getLocalCache() {
		List<MobileStatusCache> value = dataCache.poll();
		return value;
	}
	
	public void setLocalCache(List<MobileStatusCache> value) {
		dataCache.add(value);
	}
	
	public static MobilePoolLocalCache getInStance() {
		return localCache;
	}
	
	public int getCacheSize() {
		return dataCache.size();
	}
	
}

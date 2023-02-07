package com.zhongzhi.empty.task;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.zhongzhi.empty.entity.CustomerConsume;

/**
 * 消耗记录本地缓存
 * @author liuh
 * @date 2021年10月28日
 */
public class CustomerConsumeLocalCache {
	
	// 缓存ConcurrentLinkedQueue
	private static ConcurrentLinkedQueue<CustomerConsume> dataCache = new ConcurrentLinkedQueue<CustomerConsume>();
	private static CustomerConsumeLocalCache localCache = new CustomerConsumeLocalCache();
	
	private CustomerConsumeLocalCache() {
		
	}
	
	public CustomerConsume getLocalCache() {
		CustomerConsume value = dataCache.poll();
		return value;
	}
	
	public void setLocalCache(CustomerConsume value) {
		dataCache.add(value);
	}
	
	public static CustomerConsumeLocalCache getInStance() {
		return localCache;
	}
	
	public int getCacheSize() {
		return dataCache.size();
	}
	
}

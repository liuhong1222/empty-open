package com.zhongzhi.empty.redis;

import redis.clients.jedis.JedisPool;

public class DistributedLockWrapper extends DistributedLock {

	public String lockName;
	public String identifier;

    public DistributedLockWrapper(JedisPool jedisPool, String lockName, Long acquireTimeout, int timeout) {
        super(jedisPool);
        this.lockName = lockName;
        this.identifier = super.lockWithTimeout(this.lockName, acquireTimeout, timeout);
    }

    @Override
    public String lockWithTimeout(String locaName, Long acquireTimeout, int timeout) {
        this.identifier = super.lockWithTimeout(locaName, acquireTimeout, timeout);
        return this.identifier;
    }

    public boolean releaseLock() {
        if (this.identifier == null) {
            return true;
        }
        return super.releaseLock(this.lockName, this.identifier);
    }

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}

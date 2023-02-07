package com.zhongzhi.empty.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.listener.CacheRefreshListener;
import com.zhongzhi.empty.util.DingDingMessage;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;

@Slf4j
@Component
public class RedisClient {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private DingDingMessage dingDingMessage;

    @Resource
    private CacheRefreshListener subscriber;
    
    // 存对象
    public String setObject(String key, Object obj, int expireOfSeconds) throws Exception {
    	String result = "";
        try (Jedis jedis = jedisPool.getResource()) {
            ObjectOutputStream oos = null;  //对象输出流
            ByteArrayOutputStream bos = null;  //内存缓冲流
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            byte[] byt = bos.toByteArray();
            result = jedis.set(key.getBytes(), byt);
            jedis.expire(key, expireOfSeconds);
            bos.close();
            oos.close();
        } catch (Exception e) {
            log.error("jedis setObject 出错,key[" + key + "],obj[" + JSON.toJSONString(obj) + "]", e);
            dingDingMessage.sendMessage("警告：jedis set 出错,key[" + key + "],obj[" +
                    JSON.toJSONString(obj) + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }

    // 取对象
    public Object getObject(String key) throws Exception {
        Object obj = null;
        try (Jedis jedis = jedisPool.getResource()) {
            byte[] byt = jedis.get(key.getBytes());
            if (byt != null) {
                ObjectInputStream ois = null;  //对象输入流
                ByteArrayInputStream bis = null;   //内存缓冲流
                bis = new ByteArrayInputStream(byt);
                ois = new ObjectInputStream(bis);
                obj = ois.readObject();
                bis.close();
                ois.close();
            }
        } catch (Exception e) {
            log.error("jedis getObject 出错,key[" + key + "]", e);
            dingDingMessage.sendMessage("警告：jedis set 出错,key[" + key + "],e:[" +
                    e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return obj;
    }

    public String set(String key, String value) {
    	String result = "";
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.set(key, value);
//            jedis.expire(key, 30 * 60 * 1000);
        } catch (Exception e) {
            log.error("jedis set 出错,key[" + key + "],value[" + value + "]", e);
            dingDingMessage.sendMessage("警告：jedis set 出错,key[" + key + "],value[" + value + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }

    public String set(String key, String value, int expire) {
    	String result = "";
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.set(key, value);
            jedis.expire(key, expire);
        } catch (Exception e) {
            log.error("jedis set 出错,key[" + key + "],value[" + value + "],expire[" + expire + "]", e);
            dingDingMessage.sendMessage("警告：jedis set 出错,key[" + key + "],value[" + value + "],expire[" + expire + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }

    public String get(String key) {    	
        String value = "";
        try (Jedis jedis = jedisPool.getResource()) {
            value = jedis.get(key);
        } catch (Exception e) {
            log.error("jedis set 出错,key[" + key + "],value[" + value + "]", e);
            dingDingMessage.sendMessage("警告：jedis set 出错,key[" + key + "],value[" + value + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池

        return value;
    }

    public long remove(String key) {
    	long result = 0;
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.del(key);
        } catch (Exception e) {
            log.error("jedis remove 出错,key[" + key + "]", e);
            dingDingMessage.sendMessage("警告：jedis remove 出错,key[" + key + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
    
    public long incr(String key) {
    	long result = 0;
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.incr(key);
        } catch (Exception e) {
            log.error("jedis incr 出错,key[" + key + "]", e);
            dingDingMessage.sendMessage("警告：jedis incr 出错,key[" + key + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
    
    public long incrBy(String key,int value) {    	
    	long result = 0;
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.incrBy(key, value);
        } catch (Exception e) {
            log.error("jedis incrBy 出错,key[" + key + "]", e);
            dingDingMessage.sendMessage("警告：jedis incrBy 出错,key[" + key + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
    
    public long decr(String key) {
    	long result = 0;
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.decr(key);
        } catch (Exception e) {
            log.error("jedis decr 出错,key[" + key + "]", e);
            dingDingMessage.sendMessage("警告：jedis decr 出错,key[" + key + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
    
    public long decrBy(String key,int value) {
    	long result = 0;
        try (Jedis jedis = jedisPool.getResource()) {
            result = jedis.decrBy(key, value);
        } catch (Exception e) {
            log.error("jedis decrBy 出错,key[" + key + "]", e);
            dingDingMessage.sendMessage("警告：jedis decrBy 出错,key[" + key + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
    
    public long expire(String key, int value) {
    	long result = 0;
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.expire(key, value);
        } catch (Exception e) {
            log.error("jedis set 出错,key[" + key + "],value[" + value + "]", e);
            dingDingMessage.sendMessage("警告：jedis set 出错,key[" + key + "],value[" + value + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
    
    public long lpush(String key, String... strings) {
    	long result = 0;
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.lpush(key, strings);
        } catch (Exception e) {
            log.error("jedis lpush 出错,key[" + key + "],strings[" + strings + "]", e);
            dingDingMessage.sendMessage("警告：jedis lpush 出错,key[" + key + "],strings[" + strings + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
    
    public  long publish(String channel, String message) {
    	long result = 0;
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.publish(channel, message);
        } catch (Exception e) {
            log.error("jedis publish 出错,channel[" + channel + "],message[" + message + "]", e);
            dingDingMessage.sendMessage("警告：jedis publish 出错,channel[" + channel + "],message[" + message + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
	}
    
    public void subscribe(String... channels) {
        try (Jedis jedis = jedisPool.getResource()) {
        	jedis.subscribe(subscriber, channels);
        } catch (Exception e) {
            log.error("jedis subscribe 出错,channels[" + channels + "]", e);
            dingDingMessage.sendMessage("警告：jedis subscribe 出错,channels[" + channels + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
    }
    
    public Object eval(String sha1, List<String> keys, List<String> args) {
    	Object result = 0;
        try (Jedis jedis = jedisPool.getResource()) {
        	result = jedis.eval(sha1, keys, args);
        } catch (Exception e) {
            log.error("jedis eval 出错,表达式[" + sha1 + "]", e);
            dingDingMessage.sendMessage("警告：jedis eval 出错,表达式[" + sha1 + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
    
    public Object eval(String script, int keyCount, String... params) {
        Object result ="";
        try (Jedis jedis = jedisPool.getResource()) {
            result = jedis.eval(script, keyCount, params);
        } catch (Exception e) {
        	log.error("jedis eval 出错,表达式[" + script + "]", e);
            dingDingMessage.sendMessage("警告：jedis eval 出错,表达式[" + script + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
    
    public Set<String> keys(String pattern) {
    	Set<String> result = new HashSet<String>();
        try (Jedis jedis = jedisPool.getResource()) {
            result = jedis.keys(pattern);
        } catch (Exception e) {
        	log.error("jedis keys 出错,key[" + pattern + "]", e);
            dingDingMessage.sendMessage("警告：jedis keys 出错,key[" + pattern + "],e:[" + e + "]");
            throw new RuntimeException(e);
        }
        //返还到连接池
        return result;
    }
}

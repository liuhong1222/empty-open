package com.zhongzhi.empty.service.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.constants.EmptyRedisKeyConstant;
import com.zhongzhi.empty.constants.IntDirectRedisKeyConstant;
import com.zhongzhi.empty.constants.InternationalRedisKeyConstant;
import com.zhongzhi.empty.constants.RealtimeRedisKeyConstant;
import com.zhongzhi.empty.redis.RedisClient;

import lombok.extern.slf4j.Slf4j;

/**
 * 文件空号检测redis key处理实现类
 * @author liuh
 * @date 2021年10月29日
 */
@Slf4j
@Service
public class FileRedisService  {
	
	@Autowired
    private RedisClient redisClient;

	public void theTestRedisInit(Long customerId, Long emptyId, int expire, String identifier,Long fileRows) {        
		// 将标识存入redis
        redisClient.set(String.format(EmptyRedisKeyConstant.REDIS_LOCK_IDENTIFIER_KEY, customerId,emptyId), identifier, expire);
        // 初始化条数 需要进行检测的条数 检测一条 条数 + 1 累加
        redisClient.set(String.format(EmptyRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, customerId,emptyId), String.valueOf(0), expire);
        // 初始化条数 需要进行需要检测的总条数
        redisClient.set(String.format(EmptyRedisKeyConstant.TEST_COUNT_KEY, customerId,emptyId), fileRows.toString(), expire);
        // 初始化条数 多线程检测的线程数
        redisClient.set(String.format(EmptyRedisKeyConstant.GENERATE_RESULTS_KEY, customerId,emptyId), String.valueOf(0).toString(), expire);
        // 初始化 线程执行全局异常key
        redisClient.set(String.format(EmptyRedisKeyConstant.EXCEPTION_KEY, customerId,emptyId), CommonConstant.FILE_TESTING_CODE, expire);
        // 初始化 程序是否运行结束key
        redisClient.set(String.format(EmptyRedisKeyConstant.THE_RUN_KEY, customerId,emptyId), CommonConstant.FILE_TESTING_CODE, expire);
		
	}
	
	public void realtimeRedisInit(Long customerId, Long emptyId, int expire, String identifier,Long fileRows) {        
		// 将标识存入redis
        redisClient.set(String.format(RealtimeRedisKeyConstant.REDIS_LOCK_IDENTIFIER_KEY, customerId,emptyId), identifier, expire);
        // 初始化条数 需要进行检测的条数 检测一条 条数 + 1 累加
        redisClient.set(String.format(RealtimeRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, customerId,emptyId), String.valueOf(0), expire);
        // 初始化条数 需要进行需要检测的总条数
        redisClient.set(String.format(RealtimeRedisKeyConstant.TEST_COUNT_KEY, customerId,emptyId), fileRows.toString(), expire);
        // 初始化条数 多线程检测的线程数
        redisClient.set(String.format(RealtimeRedisKeyConstant.GENERATE_RESULTS_KEY, customerId,emptyId), String.valueOf(0).toString(), expire);
        // 初始化 线程执行全局异常key
        redisClient.set(String.format(RealtimeRedisKeyConstant.EXCEPTION_KEY, customerId,emptyId), CommonConstant.FILE_TESTING_CODE, expire);
        // 初始化 程序是否运行结束key
        redisClient.set(String.format(RealtimeRedisKeyConstant.THE_RUN_KEY, customerId,emptyId), CommonConstant.FILE_TESTING_CODE, expire);
		
	}
	
	public void internationalRedisInit(Long customerId, Long internationalId, int expire, String identifier,Long fileRows) {        
		// 将标识存入redis
        redisClient.set(String.format(InternationalRedisKeyConstant.REDIS_LOCK_IDENTIFIER_KEY, customerId,internationalId), identifier, expire);
        // 初始化条数 需要进行检测的条数 检测一条 条数 + 1 累加
        redisClient.set(String.format(InternationalRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, customerId,internationalId), String.valueOf(0), expire);
        // 初始化条数 需要进行需要检测的总条数
        redisClient.set(String.format(InternationalRedisKeyConstant.TEST_COUNT_KEY, customerId,internationalId), fileRows.toString(), expire);
        // 初始化条数 多线程检测的线程数
        redisClient.set(String.format(InternationalRedisKeyConstant.GENERATE_RESULTS_KEY, customerId,internationalId), String.valueOf(0).toString(), expire);
        // 初始化 线程执行全局异常key
        redisClient.set(String.format(InternationalRedisKeyConstant.EXCEPTION_KEY, customerId,internationalId), CommonConstant.FILE_TESTING_CODE, expire);
        // 初始化 程序是否运行结束key
        redisClient.set(String.format(InternationalRedisKeyConstant.THE_RUN_KEY, customerId,internationalId), CommonConstant.FILE_TESTING_CODE, expire);
		
	}
	
	public void intDirectRedisInit(Long customerId, Long intDirectId, int expire, String identifier,Long fileRows) {        
		// 将标识存入redis
        redisClient.set(String.format(IntDirectRedisKeyConstant.REDIS_LOCK_IDENTIFIER_KEY, customerId,intDirectId), identifier, expire);
        // 初始化条数 需要进行检测的条数 检测一条 条数 + 1 累加
        redisClient.set(String.format(IntDirectRedisKeyConstant.SUCCEED_TEST_COUNT_KEY, customerId,intDirectId), String.valueOf(0), expire);
        // 初始化条数 需要进行需要检测的总条数
        redisClient.set(String.format(IntDirectRedisKeyConstant.TEST_COUNT_KEY, customerId,intDirectId), fileRows.toString(), expire);
        // 初始化条数 多线程检测的线程数
        redisClient.set(String.format(IntDirectRedisKeyConstant.GENERATE_RESULTS_KEY, customerId,intDirectId), String.valueOf(0).toString(), expire);
        // 初始化 线程执行全局异常key
        redisClient.set(String.format(IntDirectRedisKeyConstant.EXCEPTION_KEY, customerId,intDirectId), CommonConstant.FILE_TESTING_CODE, expire);
        // 初始化 程序是否运行结束key
        redisClient.set(String.format(IntDirectRedisKeyConstant.THE_RUN_KEY, customerId,intDirectId), CommonConstant.FILE_TESTING_CODE, expire);
		
	}
}

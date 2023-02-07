package com.zhongzhi.empty.constants;

/**
 * 文件空号检测rediskey 常量类
 * @author liuh
 * @date 2021年10月27日
 */
public class EmptyRedisKeyConstant {

	/**
	 * key项目前缀
	 */
	public final static String CACHE_PREFIX = "kh:";
	
	/**
	 * 空号检测 检测方法加锁名称key
	 */
	public final static String THE_TEST_FUN_KEY = CACHE_PREFIX  + "ttf:%s:%s";
	
	/**
	 * 空号检测 redis锁的唯一标识key
	 */
	public final static String REDIS_LOCK_IDENTIFIER_KEY = CACHE_PREFIX  + "rli:%s:%s";
	
	/**
	 * 空号检测 已经成功检测的总条数（运行中，不考虑不计费的条数）key
	 */
	public final static String SUCCEED_TEST_COUNT_KEY = CACHE_PREFIX  + "stc:%s:%s";
	
	/**
	 * 空号检测 需要检测的总条数key （根据文件获取的总条数）
	 */
	public final static String TEST_COUNT_KEY = CACHE_PREFIX  + "tc:%s:%s";
	
	/**
	 * 空号检测 线程key（多线程执行是 全部执行完毕生成文件使用）
	 */
	public final static String GENERATE_RESULTS_KEY = CACHE_PREFIX  + "gs:ts:%s:%s";
	
	/**
	 * 空号检测全局异常key
	 */
	public final static String EXCEPTION_KEY = CACHE_PREFIX  + "gs:ex:%s:%s";
	
	/**
	 * 空号检测程序是否运行结束key
	 */
	public final static String THE_RUN_KEY = CACHE_PREFIX  + "th:rs:%s:%s";
	
	/**
	 * 空号检测已经成功检测的总条数key
	 */
	public final static String SUCCEED_CLEARING_COUNT_KEY = CACHE_PREFIX  + "scc:%s:%s";
	
	/**
	 * 空号检测需要显示在前端进度页面上的号码key
	 */
	public final static String DEFAULT_MOBILE_DISPLAY_KEY = CACHE_PREFIX  + "dmdk:%s:%s";
	
	/**
	 * 空号检测需要显示在前端进度页面上的号码key
	 */
	public final static String MOBILE_DISPLAY_KEY = CACHE_PREFIX  + "mdk:%s:%s";
}

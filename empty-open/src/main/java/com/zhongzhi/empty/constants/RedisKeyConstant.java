package com.zhongzhi.empty.constants;

/**
 * rediskey 常量类
 * @author liuh
 * @date 2021年10月27日
 */
public class RedisKeyConstant {

	/**
	 * key项目前缀
	 */
	public final static String CACHE_PREFIX = "eo:";
	
	/**
	 * key模块前缀
	 */
	public final static String CACHE_VERSION = "balance:";
	
	/**
	 * 空号检测余额key
	 */
	public final static String EMPTY_BALANCE_KEY = CACHE_PREFIX + CACHE_VERSION + "empty:";
	
	/**
	 * 空号检测冻结金额key
	 */
	public final static String EMPTY_FREEZE_AMOUNT_KEY = CACHE_PREFIX + CACHE_VERSION + "efak:";
	
	/**
	 * 空号检测实际扣款金额key
	 */
	public final static String EMPTY_REAL_DEDUCT_FEE_KEY = CACHE_PREFIX + CACHE_VERSION + "erdfk:";
	
	/**
	 * 实时检测余额key
	 */
	public final static String REALTIME_BALANCE_KEY = CACHE_PREFIX + CACHE_VERSION + "realtime:";
	
	/**
	 * 实时检测冻结金额key
	 */
	public final static String REALTIME_FREEZE_AMOUNT_KEY = CACHE_PREFIX + CACHE_VERSION + "rfak:";
	
	/**
	 * 实时检测实际扣款金额key
	 */
	public final static String REALTIME_REAL_DEDUCT_FEE_KEY = CACHE_PREFIX + CACHE_VERSION + "rrdfk:";
	
	/**
	 *国际检测余额key
	 */
	public final static String INTERNATIONAL_BALANCE_KEY = CACHE_PREFIX + CACHE_VERSION + "international:";
	
	/**
	 * 国际检测冻结金额key
	 */
	public final static String INTERNATIONAL_FREEZE_AMOUNT_KEY = CACHE_PREFIX + CACHE_VERSION + "ifak:";
	
	/**
	 * 国际检测实际扣款金额key
	 */
	public final static String INTERNATIONAL_REAL_DEDUCT_FEE_KEY = CACHE_PREFIX + CACHE_VERSION + "irdfk:";
	
	/**
	 * 限流出现超限的key
	 */
	public final static String CURRENT_LIMIT_KEY = CACHE_PREFIX + CACHE_VERSION + "clk:";
	
	/**
	 * 文件md5缓存key
	 */
	public final static String FILE_MD5_CACHE_KEY = "file:fmck:%s:%s";
	
	/**
	 * 文件实际调用接口条数缓存key
	 */
	public final static String FILE_REAL_API_CACHE_KEY = "file:frack:%s:%s";
	
	/**
	 * 定向通用检测余额key
	 */
	public final static String DIRECT_COMMON_BALANCE_KEY = CACHE_PREFIX + CACHE_VERSION + "directCommon:";
	
	/**
	 *定向通用检测冻结金额key
	 */
	public final static String DIRECT_COMMON_FREEZE_AMOUNT_KEY = CACHE_PREFIX + CACHE_VERSION + "dcfak:";
	
	/**
	 * 定向通用检测实际扣款金额key
	 */
	public final static String DIRECT_COMMON_REAL_DEDUCT_FEE_KEY = CACHE_PREFIX + CACHE_VERSION + "dcrdfk:";
	
	/**
	 * line定向检测余额key
	 */
	public final static String LINE_DIRECT_BALANCE_KEY = CACHE_PREFIX + CACHE_VERSION + "lineDirect:";
	
	/**
	 * line定向检测冻结金额key
	 */
	public final static String LINE_DIRECT_FREEZE_AMOUNT_KEY = CACHE_PREFIX + CACHE_VERSION + "ldfak:";
	
	/**
	 * line定向检测实际扣款金额key
	 */
	public final static String LINE_DIRECT_REAL_DEDUCT_FEE_KEY = CACHE_PREFIX + CACHE_VERSION + "ldrdfk:";
}

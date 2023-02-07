package com.zhongzhi.empty.constants;

/**
 * 公共常量类
 * @author liuh
 * @date 2021年10月26日
 */
public class CommonConstant {

	/**
	 * 空号检测通道参数名称
	 */
	public final static String EMPTY_GATEWAY = "empty_gateway";
	
	/**
	 * 空号检测在线默认通道
	 */
	public final static String EMPTY_GATEWAY_ONLINE_DEFAULT = "chuanglan";
	
	/**
	 * 空号检测通道-创蓝
	 */
	public final static String CHUANGLAN_EMPTY_GATEWAY = "chuanglan";
	
	/**
	 * 空号检测通道-缓存池
	 */
	public final static String LOCAL_EMPTY_GATEWAY = "local";
	
	/**
	 * 空号检测成功code
	 */
	public final static String UNN_SUCCESS_CODE = "000000";
	
	/**
	 * 实时成功code
	 */
	public final static String REALTIME_SUCCESS_CODE = "200000";
	
	/**
	 * 国际成功code
	 */
	public final static String INTERNATIONAL_SUCCESS_CODE = "100";
	
	/**
	 * 空号检测号码计费状态
	 */
	public final static String MOBILE_CHARGE_STATUS = "1";
	
	/**
	 * 沉默的号码状态
	 */
	public final static String SLIENCE_MOBILE_STATUS = "4";
	
	/**
     * 用户上传待分类的号码文件名
     */
	public final static String SOURCE_FILE_NAME = "source.txt";
	
	/**
	 * 空号检测检测中的code
	 */
	public final static String FILE_TESTING_CODE = "000000";
	
	/**
	 * 空号检测检测失败的code
	 */
	public final static String FILE_TEST_FAILED_CODE = "999999";
	
	/**
	 * 风险号的号码颜色
	 */
	public final static String TESTPROCESS_MOBILECOLOR_RED = "red";	
	/**
	 * 沉默号的号码颜色
	 */
	public final static String TESTPROCESS_MOBILECOLOR_YELLOW = "yellow";	
	/**
	 * 空号的号码颜色
	 */
	public final static String TESTPROCESS_MOBILECOLOR_GRAY = "gray";
	/**
	 * 实号的号码颜色
	 */
	public final static String TESTPROCESS_MOBILECOLOR_BLUE = "blue";
	
	/**
	 * 正在检测中
	 */
	public final static String THETEST_RUNNING = "1";
	/**
	 * 检测完成
	 */
	public final static String THETEST_FINISH = "2";
	/**
	 * 检测异常
	 */
	public final static String THETEST_EXCEPTION = "3";	
	/**
	 * 没有正在检测的任务
	 */
	public final static String THETEST_NULL = "6";
	
	/**
	 *redis新增key成功的返回值
	 */
	public final static String REDIS_SET_RETURN = "OK";
	
	/**
     * 活跃号码文件名
     */
	public final static String ACTIVE_FILE_NAME = "活跃号(实号).txt";

    /**
     * 静默号码文件名
     */
	public final static String SILENT_FILE_NAME = "沉默号.txt";

    /**
     * 风险号码文件名
     */
	public final static String RISK_FILE_NAME = "风险号.txt";

    /**
     * 空号码文件名
     */
	public final static String EMPTY_FILE_NAME = "空号.txt";
	
	/**
	 * 实时不收费的code
	 */
	public final static String REALTIME_FREE_CHARGE_CODE = "9,10";
	
	/**
	 * 实时接口是携号转网标识
	 */
	public final static String REALTIME_MNP_STATUS = "1";
	
	/**
	 * 实时接口正常结果状态
	 */
	public final static String REALTIME_NORMAL_STATUS = "1";
	
	/**
     * 正常文件名
     */
	public final static String NORMAL_FILE_NAME = "正常.txt";

    /**
     * 空号文件名
     */
	public final static String REALTIME_EMPTY_FILE_NAME = "空号.txt";

    /**
     * 通话中文件名
     */
	public final static String ONCALL_FILE_NAME = "通话中.txt";

    /**
     * 不在网文件名
     */
	public final static String NOT_ONLINE_FILE_NAME = "不在网(空号).txt";
	
	/**
     * 关机文件名
     */
	public final static String SHUTDOWN_FILE_NAME = "关机.txt";
	
	/**
     * 疑似关机文件名
     */
	public final static String LIKE_SHUTDOWN_FILE_NAME = "疑似关机.txt";

    /**
     * 停机文件名
     */
	public final static String TINGJI_FILE_NAME = "停机.txt";

    /**
     * 携号转网文件名
     */
	public final static String MNP_FILE_NAME = "携号转网.txt";

    /**
     * 号码错误文件名
     */
	public final static String MOBILE_ERROR_FILE_NAME = "号码错误.txt";
	
	/**
     * 未知文件名
     */
	public final static String UNKNOWN_FILE_NAME = "未知.txt";
	
	/**
     * 老系统接口成功code
     */
	public final static String OLD_SYSTEM_SUCCESS_CODE = "200";
	
	/**
     * 已激活文件名
     */
	public final static String ACTIVE_IN_FILE_NAME = "已激活.txt";
	
	/**
     * 未激活文件名
     */
	public final static String NO_ACTIVE_IN_FILE_NAME = "未激活.txt";
	
	/**
     * 未注册文件名
     */
	public final static String NO_REGISTER_FILE_NAME = "未注册.txt";
	
	/**
	 *印度国际码号
	 */
	public final static String YINDU_COUNTRY_CODE = "91";
	
	/**
	 *何柳强国际检测接口成功码
	 */
	public final static String HLQ_SUCCESS_STATUS = "000";
}

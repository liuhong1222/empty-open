package com.zhongzhi.empty.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 时间处理类【存储系统所有的日期时间处理函数】
 * @author liuh
 * @date 2021年11月1日
 */
public class DateUtils {

	private final static Logger logger = LoggerFactory.getLogger(DateUtils.class);

	private static final String FORMAT_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

	private static final List<String> formarts = new ArrayList<>(10);

	static {
		formarts.add("yyyy-MM");
		formarts.add("yyyy-MM-dd");
		formarts.add("yyyy-MM-dd hh:mm");
		formarts.add("yyyy-MM-dd hh:mm:ss");
		formarts.add("yyyy-MM-dd hh:mm:ss.SSS");

		formarts.add("yyyy/MM");
		formarts.add("yyyy/MM/dd");
		formarts.add("yyyy/MM/dd hh:mm");
		formarts.add("yyyy/MM/dd hh:mm:ss");
		formarts.add("yyyy/MM/dd hh:mm:ss.SSS");
	}

	/**
	 * @return 获取当前时间格式 yyyyMMddHHmmssms
	 */
	public static String getDateTimeS() {
		DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		return df.format(new Date());
	}
	
	/**
	 * 字符串转date，格式自动识别
	 */
	public static Date convert(String source) {
		if (StringUtils.isBlank(source)) {
			logger.error("时间格式不正确,source 为空");
			return new Date();
		}
		String value = source.trim();
		if (source.matches("^\\d{4}-\\d{1,2}$")) {
			return parseDate(value, formarts.get(0));
		} else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
			return parseDate(value, formarts.get(1));
		} else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}$")) {
			return parseDate(value, formarts.get(2));
		} else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$")) {
			return parseDate(value, formarts.get(3));
		} else if (source.matches("^\\d{4}-\\d{1,2}-\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}.\\d{1,3}$")) {
			return parseDate(value, formarts.get(4));
		} else if (source.matches("^\\d{4}/\\d{1,2}$")) {
			return parseDate(value, formarts.get(5));
		} else if (source.matches("^\\d{4}/\\d{1,2}/\\d{1,2}$")) {
			return parseDate(value, formarts.get(6));
		} else if (source.matches("^\\d{4}/\\d{1,2}/\\d{1,2} {1}\\d{1,2}:\\d{1,2}$")) {
			return parseDate(value, formarts.get(7));
		} else if (source.matches("^\\d{4}/\\d{1,2}/\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}$")) {
			return parseDate(value, formarts.get(8));
		} else if (source.matches("^\\d{4}/\\d{1,2}/\\d{1,2} {1}\\d{1,2}:\\d{1,2}:\\d{1,2}.\\d{1,3}$")) {
			return parseDate(value, formarts.get(9));
		} else {
			logger.error("时间格式无法识别,source:{}",source);
			return new Date();
			//throw new IllegalArgumentException("Invalid boolean value '" + source + "'");
		}
	}

	public static Date converYYYYMMddHHmmssStrToDate(String date){
		Date d = null;
		try {
			if(date == null || "".equals(date.trim()))
				return null;
			SimpleDateFormat format = new SimpleDateFormat(FORMAT_YYYY_MM_DD_HH_MM_SS,Locale.ENGLISH);
			d = format.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return d;
	}

	/**
	 * 按照指定的格式，将日期类型对象转换成字符串，例如：yyyy-MM-dd,yyyy/MM/dd,yyyy/MM/dd hh:mm:ss
	 *
	 * @param date
	 * @param pattern
	 *            格式
	 * @return
	 */
	public static String formatDate(Date date, String pattern) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat formater = new SimpleDateFormat(pattern);
		return formater.format(date);
	}

	/**
	 * 按照yyyy-MM-dd格式，将 Date 对象转换成字符串
	 *
	 * @param date
	 * @param pattern
	 *            格式
	 * @return
	 */
	public static String formatDate(Date date) {
		if (date == null) {
			return "";
		}
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd");
		return formater.format(date);
	}

	/**
	 * 按照yyyy-MM-dd格式，将 Date 对象转换成字符串
	 *
	 * @param date
	 * @param pattern
	 *            格式
	 * @return
	 */
	public static String formatDateByDet(Date date) {
		if (date == null) {
			return "";
		}
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return formater.format(date);
	}

	/**
	 * 按照yyyy-MM格式，将 Date 对象转换成字符串
	 *
	 * @param date
	 * @param pattern
	 *            格式
	 * @return
	 */
	public static String formatDateType(Date date) {
		if (date == null) {
			return "";
		}
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM");
		return formater.format(date);
	}

	/**
	 * 按照yyyy-MM-dd格式，将Timestamp对象转换成字符串
	 *
	 * @param date
	 * @return
	 */
	public static String formatDate(Timestamp timestamp) {

		if (timestamp == null) {
			return "";
		}

		return DateUtils.formatDate(new Date(timestamp.getTime()));

	}

	/**
	 * 按照yyyy-MM-dd格式，将Timestamp对象转换成字符串
	 *
	 * @param date
	 * @return
	 */
	public static String formatDateByDeatail(Timestamp timestamp) {

		if (timestamp == null) {
			return "";
		}

		return DateUtils.formatDateByDet(new Date(timestamp.getTime()));

	}

	/**
	 * 按照指定的格式，将字符串转换成日期类型对象，例如：yyyy-MM-dd,yyyy/MM/dd,yyyy/MM/dd hh:mm:ss
	 *
	 * @param dateStr
	 * @param pattern
	 * @return
	 */
	public static Date parseDate(String dateStr, String pattern) {
		SimpleDateFormat formater = new SimpleDateFormat(pattern);
		try {
			return formater.parse(dateStr);
		} catch (ParseException e) {
			logger.error("时间格式无法识别,dateStr:{},pattern:{},message:{}",dateStr,pattern,e);
		}
		return null;
	}

	/**
	 * 将字符串（yyyy-MM-dd）解析成Date
	 *
	 * @param dateStr
	 * @return
	 */
	public static Date parseDate(String dateStr) {
		return parseDate(dateStr, "yyyy-MM-dd");
	}

	/**
	 * 将字符串（yyyy-MM-dd）解析成Date
	 *
	 * @param dateStr
	 * @return
	 */
	public static Date parseDateAll(String dateStr) {
		return parseDate(dateStr, "yyyy-MM-dd HH:mm");
	}

	/**
	 * 将字符串（yyyy-MM-dd）解析成Timestamp
	 *
	 * @param timeStampStr
	 * @return
	 */
	public static Timestamp parseTimestamp(String timeStampStr) {

		Date date = parseDate(timeStampStr, "yyyy-MM-dd");
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		if (date != null) {

			return Timestamp.valueOf(formater.format(date));
		}

		return null;
	}

	/**
	 * 将Date (yyyy-MM-dd)解析成Timestamp
	 *
	 * @param timeStampStr
	 * @return
	 */
	public static Timestamp parseTimestamp(Date date) {
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (date != null) {

			return Timestamp.valueOf(formater.format(date));
		}

		return null;
	}

	/**
	 * 给定的日期字符串加1天 并返回Timestamp类型
	 *
	 * @param date<yyyy-MM-dd>
	 * @return
	 */
	public static Timestamp addOneDay(String date) {
		Date d = DateUtils.parseDate(date);
		Calendar ca = Calendar.getInstance();
		ca.setTime(d);
		ca.add(Calendar.DAY_OF_MONTH, 1);
		return parseTimestamp(ca.getTime());
	}

	/**
	 * 给定的日期字符串加addDay天 并返回Timestamp类型
	 *
	 * @param date<yyyy-MM-dd>
	 * @return
	 */
	public static Timestamp addDay(String date, int addDay) {

		Date d = DateUtils.parseDate(date);
		Calendar ca = Calendar.getInstance();
		ca.setTime(d);
		ca.add(Calendar.DAY_OF_MONTH, addDay);

		return parseTimestamp(ca.getTime());
	}

	/**
	 * 给定的日期增加addDay天 并返回Date类型
	 *
	 * @param date
	 * @param addDay
	 * @return
	 */
	public static Date addDay(Date date, int addDay) {

		Calendar ca = Calendar.getInstance();
		ca.setTime(date);
		ca.add(Calendar.DAY_OF_MONTH, addDay);

		return ca.getTime();
	}
	
	/**
	 * 获取随机日期
	 *
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static Date randomDate(Date beginDate, Date endDate) {
		
		if(beginDate.getTime() >= endDate.getTime()){
			return null;
		}
		//获取随机时间
		long date = random(beginDate.getTime(),endDate.getTime());
		return new Date(date);
	}
	
	private static long random(long begin,long end){
		long rtn = begin + (long)(Math.random() * (end - begin));
		if(rtn == begin || rtn == end){
			return random(begin,end);
		}
		return rtn;
	}

	/**
	 * 给定的日期增加addMonth月，并返回Date类型
	 *
	 * @param date
	 * @param addDay
	 * @return
	 */
	public static Date addMonth(Date date, int addMonth) {

		Calendar ca = Calendar.getInstance();
		ca.setTime(date);
		ca.add(Calendar.MONTH, addMonth);

		return ca.getTime();
	}

	/**
	 * 日期推算去除周六日
	 *
	 * @param date
	 * @param workDay
	 * @return
	 */
	public static String getWorkDate(Date date, int workDay) {
		Calendar cal = Calendar.getInstance();
		String pattern = "yyyy-MM-dd";
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		int days = workDay % 5 + workDay / 5 * 7;
		cal.setTime(date);
		cal.add(Calendar.DATE, days);
		return sdf.format(cal.getTime());
	}

	/**
	 * 计算两个日期之间相差的天数
	 *
	 * @param startday
	 * @param endday
	 * @return
	 */
	public static int getIntervalDaysByDate(Date startday, Date endday) {
		if (startday.after(endday)) {
			Date cal = startday;
			startday = endday;
			endday = cal;
		}
		long sl = startday.getTime();
		long el = endday.getTime();
		long ei = el - sl;
		return (int) (ei / (1000 * 60 * 60 * 24));
	}

	/**
	 * 获取两日期的差值 @author Kevin Li @date 2015年1月27日 上午9:41:50 @Title
	 * getItvDaysByDate @Description @param startday @param endday @return
	 * int @throws
	 */
	public static int getItvDaysByDate(Date startday, Date endday) {
		long sl = startday.getTime();
		long el = endday.getTime();
		long ei = el - sl;
		return (int) ei;
	}

	/**
	 * 计算两个日期之间相差的小时数 @author Kevin Li @date 2014年9月12日 下午3:31:12 @Title
	 * getIntervalHour @Description @param startday @param endday @return
	 * int @throws
	 */
	public static int getIntervalHour(Date startday, Date endday) {
		long interval = endday.getTime() - startday.getTime();
		long day = interval / (24 * 60 * 60 * 1000);
		long hour = day * 24 + (interval / (60 * 60 * 1000) - day * 24);
		return (int) hour;
	}

	/**
	 * 设置时间为这一天的第一毫秒
	 *
	 * @param date
	 * @return
	 */
	public static Date setDateToOneDayFirstMilliSecond(Date date) {

		Calendar ca = Calendar.getInstance();
		ca.setTime(date);
		ca.set(Calendar.HOUR_OF_DAY, ca.getActualMinimum(Calendar.HOUR_OF_DAY));
		ca.set(Calendar.MINUTE, ca.getActualMinimum(Calendar.MINUTE));
		ca.set(Calendar.SECOND, ca.getActualMinimum(Calendar.SECOND));
		ca.set(Calendar.MILLISECOND, ca.getActualMinimum(Calendar.MILLISECOND));

		return ca.getTime();
	}

	/**
	 * 设置时间为这一天的最后一毫秒
	 *
	 * @param date
	 * @return
	 */
	public static Date setDateToOneDayLastMilliSecond(Date date) {

		Calendar ca = Calendar.getInstance();
		ca.setTime(date);
		ca.set(Calendar.HOUR_OF_DAY, ca.getActualMaximum(Calendar.HOUR_OF_DAY));
		ca.set(Calendar.MINUTE, ca.getActualMaximum(Calendar.MINUTE));
		ca.set(Calendar.SECOND, ca.getActualMaximum(Calendar.SECOND));
		ca.set(Calendar.MILLISECOND, ca.getActualMaximum(Calendar.MILLISECOND));

		return ca.getTime();
	}

	/**
	 * 取得当前年[Integer类型]
	 *
	 * @return
	 */
	public static Integer getCurrentYear() {
		Calendar calendar = new GregorianCalendar();
		// 得到当前年
		return calendar.get(Calendar.YEAR);
	}

	/**
	 * 取得格式化后的当前系统时间[字符串类型]
	 *
	 * @return
	 */
	public static String getCurrentSytemTimeForString() {
		Calendar calendar = new GregorianCalendar();
		String pattern = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		String systemTime = format.format(calendar.getTime());
		return systemTime;
	}

	/**
	 * 取得格式化后的当前系统时间[Timestamp类型]
	 *
	 * @return
	 */
	public static Timestamp getCurrentSytemTimeForTimestamp() {
		return Timestamp.valueOf(getCurrentSytemTimeForString());
	}

	/**
	 * 判断当前日期是否超过 一年
	 *
	 * @param hireDate
	 * @return
	 */
	public static String checkOverCurrentOneYear(Date hireDate) {

		String returnValue = null;

		java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");

		Calendar c = Calendar.getInstance();
		String currentDay = df.format(c.getTime());

		Date day_curDate = parseDate(currentDay);

		int year = getIntervalDaysByDate(day_curDate, hireDate);

		if (year > 365) {
			returnValue = "overOneYears";
		}
		return returnValue;

	}

	/**
	 * 取得当前时间
	 *
	 * @return
	 */
	public static Date getCurrentDateTime() throws ParseException {
		Calendar calendar = new GregorianCalendar();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date currentDate = calendar.getTime();
		return format.parse(format.format(currentDate));
	}

	/**
	 * 根据beginDate和endDate日期判断currentDate是否在这二个日期之间
	 *
	 * @param current
	 * @param beginDate
	 * @param endDate
	 * @return
	 */
	public static boolean checkCurrentDateTheTwoTimeBetween(Date currentDate, Date beginDate, Date endDate) {

		Boolean flag = false;

		if (currentDate.getTime() >= beginDate.getTime() && currentDate.getTime() <= endDate.getTime()) {
			flag = true;
		}
		return flag;
	}

	/** 
     * 计算两个日期之间相差的天数 
     * @param smdate 较小的时间
     * @param bdate  较大的时间
     * @return 相差天数
     * @throws ParseException 
     */  
    public static int daysBetween(Date smdate,Date bdate)  {
    	try {
    		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
            smdate=sdf.parse(sdf.format(smdate));
            bdate=sdf.parse(sdf.format(bdate));
            Calendar cal = Calendar.getInstance();  
            cal.setTime(smdate);  
            long time1 = cal.getTimeInMillis();               
            cal.setTime(bdate);  
            long time2 = cal.getTimeInMillis();       
            long between_days=(time2-time1)/(1000*3600*24);
            return Integer.parseInt(String.valueOf(between_days));   
		} catch (Exception e) {
			logger.error("日期转换异常，smDate:{},bDate:{},info:",smdate,bdate,e);
			return 0;
		}
              
    } 
    
	/**
	 * 判断当前日期是周六或者周末
	 *
	 * @return
	 */
	public static boolean getcurrentWeek() {
		boolean returnValue = true;
		Calendar calendar = Calendar.getInstance();
		Date date = new Date();
		calendar.setTime(date);
		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		int value = dayOfWeek - 1;
		if (value == 0 || value == 6) {
			returnValue = false;
		}
		return returnValue;
	}

	/**
	 * 按照yyyy-MM-dd HH:mm:ss EEE格式，将 Date 对象转换成字符串
	 *
	 * @param date
	 * @param pattern
	 *            格式
	 * @return
	 */
	public static String formatDateWeek(Date date) {
		if (date == null) {
			return "";
		}
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss EEE");
		return formater.format(date);
	}

	/**
	 * @return 获取当前时间格式 yyyyMMddHHmmssms
	 */
	public static String getDateTime() {
		DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssms");
		return df.format(new Date());
	}
	
	/**
	 * @return 获取当前时间格式 yyyyMMddHHmmssms
	 */
	public static Long getLongTime(Date date) {
		DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		return Long.valueOf(df.format(date));
	}

	/**
	 * @return 获取当前时间格式 yyyyMMdd
	 */
	public static String getDate() {
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		return df.format(new Date());
	}

	/**
	 * @return 获取当前时间格式 yyyy-MM-dd
	 */
	public static String getToday() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		return df.format(new Date());
	}

	/**
	 * @return 获取上月日期格式 yyyy-MM
	 */
	public static String getLastMonth() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MONTH, -1);

		return df.format(cal.getTime());
	}

	// 全局的获取当前时间
	public static String getNowTime() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format(new Date());
	}

	// 全局的获取当前时间
	public static String getNowTime1() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return df.format(new Date());
	}

	// 全局的获取当前时间
	public static Date getNowDate() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return df.parse(df.format(new Date()));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	// 全局的获取当前时间
		public static Date getNowDate1() {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			try {
				return df.parse(df.format(new Date()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return null;
		}

	/**
	 * 返回格式化为yyyyMMdd的昨天日期
	 *
	 * @param date
	 * @return java.lang.String
	 */
	public static String getYesterday() {

		Calendar ca = Calendar.getInstance();
		ca.setTime(new Date());
		ca.add(Calendar.DAY_OF_MONTH, -1);
		return formatDate(ca.getTime());
	}

	/**
	 * 格式化一个日期字符串，格式为yyyyMMdd
	 * @return
	 */
	public static String formatDate(String dateStr, String pattern) {
		Date dd;
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			dd = formater.parse(dateStr);
		} catch (ParseException e) {
			formater = new SimpleDateFormat("yyyyMMdd");
			try {
				dd = formater.parse(dateStr);
			} catch (ParseException e1) {
				return "";
			}
		}
		if (dd == null) {
			return "";
		}
		formater = new SimpleDateFormat(pattern);
		return formater.format(dd);
	}

	/**
	 * 今年月份，
	 *
	 * @return
	 */
	public static int getMonth() {
		return Calendar.getInstance().get(Calendar.MONTH);
	}

	/**
	 * 格式化时间，对结束时间增加23h59m59s @author Kevin Li @date 2015年1月8日 下午4:20:20 @Title
	 * fmtHSM @Description @param date @return Date @throws
	 */
	public static Date fmtHSM(Date date) {
		Date reDate = null;
		if (null != date) {
			Calendar ca = Calendar.getInstance();
			ca.setTime(date);
			ca.add(Calendar.HOUR_OF_DAY, 23);
			ca.add(Calendar.SECOND, 59);
			ca.add(Calendar.MINUTE, 59);
			reDate = ca.getTime();
		}
		return reDate;
	}

	/**
	 * 格式化时间，对结束时间增加23h59m59s @author Kevin Li @date 2015年1月8日 下午4:20:20 @Title
	 * fmtHSM @Description @param date @return Date @throws
	 */
	public static Date fmtHSMStart(Date date) {
		Date reDate = null;
		if (null != date) {
			Calendar ca = Calendar.getInstance();
			ca.setTime(date);
			ca.add(Calendar.HOUR_OF_DAY, 0);
			ca.add(Calendar.SECOND, 0);
			ca.add(Calendar.MINUTE, 0);
			reDate = ca.getTime();
		}
		return reDate;
	}

	/**
	 * 获取昨天的时间 到23h59m59s @author liming @date 2015年4月7日 下午4:41:00 @Title
	 * getYesterdayEnd @Description @return Date @throws
	 */
	public static Date getYesterdayEnd() {
		Calendar ca = Calendar.getInstance();
		ca.setTime(new Date());
		ca.add(Calendar.DAY_OF_MONTH, -1);
		return fmtHSM(ca.getTime());
	}

	/**
	 * 获取昨天的时间 到0h0m0s @author liming @date 2015年4月7日 下午4:41:42 @Title
	 * getYesterdayStart @Description @return Date @throws
	 */

	public static Date getYesterdayStart() {
		Calendar ca = Calendar.getInstance();
		ca.setTime(new Date());
		ca.add(Calendar.DAY_OF_MONTH, -1);
		return fmtHSMStart(ca.getTime());
	}

	/**
	 * 获取当前时间加半小时 @author liming @date 2015年4月7日 下午4:41:42 @Title
	 * getYesterdayStart @Description @return Date @throws
	 */

	public static String getCurrentTimeMillis() {
		long curren = System.currentTimeMillis();
		curren += 30 * 60 * 1000;
		Date date = new Date(curren);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(date);
	}

	/**
	 * 获取限制的警告时间 @author 2881526645@qq.com @date 2017年3月6日 下午4:04:48 @Title
	 * getLimitAlarmTime @Description @param date @param flag @return
	 * Date @throws
	 */
	public static Date getLimitAlarmTime(Date date, int flag) {

		Calendar cal = Calendar.getInstance();

		cal.setTime(date);

		int hour = cal.get(Calendar.HOUR_OF_DAY);

		int minute = cal.get(Calendar.MINUTE);

		int second = cal.get(Calendar.SECOND);

		// 时分秒（毫秒数）

		long millisecond = hour * 60 * 60 * 1000 + minute * 60 * 1000 + second * 1000;

		// 凌晨00:00:00

		cal.setTimeInMillis(cal.getTimeInMillis() - millisecond);

		if (flag == 0) {

			return cal.getTime();

		} else if (flag == 1) {

			// 凌晨23:59:59

			cal.setTimeInMillis(cal.getTimeInMillis() + 7 * 60 * 60 * 1000);

		}

		return cal.getTime();

	}

	/**
	 *
	 * @author 2881526645@qq.com @date 2017年3月7日 上午11:03:27 @Title
	 * getLimitAlarmTimeXx @Description @param date 当前日期的时间 @param
	 * timeIntervalHour 时间间隔 单位是小时 @return long @throws
	 */
	public static long getLimitAlarmTimeXx(Date date, int timeIntervalHour) {

		long morningTime00 = DateUtils.parseDate(DateUtils.formatDate(date, "yyyy-MM-dd 00:00:00")).getTime();

		if (timeIntervalHour == 0) {
			return morningTime00;
		} else {
			morningTime00 = morningTime00 + timeIntervalHour * 60 * 60 * 1000;

		}

		return morningTime00;

	}

	/**
	 * 获取当月第一天 @author 2881526645@qq.com @date 2017年5月15日 上午10:32:28 @Title
	 * getCurrentMonthFirstDay @Description @return String @throws
	 */
	public static String getCurrentMonthFirstDay() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, 0);
		c.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
		return format.format(c.getTime());
	}

	/**
	 * 获取凌晨时间点 @author 2881526645@qq.com @date 2017年5月15日 上午10:35:22 @Title
	 * getMorningTime @Description @param hour 凌晨的时间点 @return long @throws
	 */
	@SuppressWarnings("unused")
	private static long getMorningTime(int hour) {
		long ONE_DAY = 24 * 60 * 60 * 1000;
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long ret = cal.getTimeInMillis();
		return ret + ONE_DAY;
	}

	/**
	 * 给指定时间加8个小时
	 * @return
	 */
	public static String DateAddhours(String day, int hour){
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			date = format.parse(day);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (date == null)
			return "";
		System.out.println("front:" + format.format(date)); //显示输入的日期
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR, hour);// 24小时制
		date = cal.getTime();
		System.out.println("after:" + format.format(date));  //显示更新后的日期
		cal = null;
		return format.format(date);
	}

	/**
	 * 将格式yyyyMMddHHmmss 转换为Date类型
	 * @param time
	 * @return
	 * @throws ParseException
	 */
	public static Date StringToDate(String time) throws ParseException{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		// SimpleDateFormat的parse(String time)方法将String转换为Date
		return simpleDateFormat.parse(time);
	}
	
	/**
	 * 获取所在月的最后一天
	 * 
	 * @return
	 */
	public static Date getCurrentMonthLastDay(Date start) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(start);
		int endday = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
		calendar.set(Calendar.DATE, endday);
		return calendar.getTime();
	}
	
	/**
	 * 获取所在月的第一天
	 * 
	 * @return
	 */
	public static Date getCurrentMonthFirstDay(Date start) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(start);
		int startday = calendar.getActualMinimum(Calendar.DAY_OF_MONTH);
		calendar.set(Calendar.DATE, startday);
		return calendar.getTime();
	}

	public static List<Date> dateSplit(Date startDate, Date endDate)
	        throws Exception {
	    if (!startDate.before(endDate))
	        throw new Exception("开始时间应该在结束时间之后");
	    Long spi = endDate.getTime() - startDate.getTime();
	    Long step = spi / (24 * 60 * 60 * 1000);// 相隔天数

	    List<Date> dateList = new ArrayList<Date>();
	    dateList.add(endDate);
	    for (int i = 1; i <= step; i++) {
	        dateList.add(new Date(dateList.get(i - 1).getTime()
	                - (24 * 60 * 60 * 1000)));// 比上一天减一
	    }
	    return dateList;
	}
	public static void main(String[] args) throws Exception {
//
//		// 1494988003000
//		// System.out.println(DateUtils.formatDate(new
//		// Date(Long.valueOf(1494988003000l)), "yyyy-MM-dd hh:mm:ss"));
//
////		System.out.println(DateUtils.parseDate("2017-01-05 12:27:23", "yyyy-MM-dd hh:mm:ss"));
//
//
		        System.out.println( getNowDate1());
//
	}

}

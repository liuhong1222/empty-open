package com.zhongzhi.empty.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @author liuh
 * @date 2021年11月3日
 */
public interface RealtimeReportGroupEnum {

    /**
     * 实时文件检测 未知1级:大数据有返回状态，但数据库没有匹配 未知2级：大数据没有返回状态
     */
    enum FileDetection implements RealtimeReportGroupEnum {
        //正常
    	NORMAL("normal", "1"),
        //空号
    	EMPTY("kong", "2"),
        //通话中
    	ON_CALL("oncall", "3"),
        //不在网
    	NOT_ONLINE("notonline", "4"),
        //关机
    	GUANJI("guanji", "5"),
        //疑似关机
    	LIKE_GUANJI("likeguanji", "7"),
        //停机
    	TINGJI("tingji", "13"),
        //携号转网
    	MNP("mnp", "11"),
        //号码错误
    	MOBILE_ERROR("mobileerror", "12"),
        //未知
    	UNKNOWN("unknown", "9,10"),;
        /**
         * 组别号
         */
        private String groupCode;
        /**
         * 后置组别号
         */
        private String backGroupCode;


        FileDetection(String groupCode, String backGroupCode) {
            this.groupCode = groupCode;
            this.backGroupCode = backGroupCode;
        }


        public String getGroupCode() {
            return groupCode;
        }


        public String getBackGroupCode() {
            return backGroupCode;
        }


        /**
         * 根据组别号返回枚举
         *
         * @param groupCode
         * @return
         */
        public static FileDetection fromGroupCode(String groupCode) {
            for (FileDetection fileDetection : FileDetection.values()) {
                if (fileDetection.getGroupCode().equals(groupCode)) {
                    return fileDetection;
                }
            }
            return null;
        }


        /**
         * 根据后置组别号返回枚举
         *
         * @param backGroupCode
         * @return
         */
        public static FileDetection fromBackGroupCode(String backGroupCode) {
            for (FileDetection fileDetection : FileDetection.values()) {
                if (fileDetection.getBackGroupCode().equals(backGroupCode)) {
                    return fileDetection;
                }
            }
            return null;
        }


        /**
         * 根据前置组别号返回后置枚举
         *
         * @param groupCode
         * @return
         */
        public static FileDetection getBackGroupEnum(String groupCode) {
            FileDetection fd = fromGroupCode(groupCode);
            if (fd != null && StringUtils.isNotBlank(fd.getBackGroupCode())) {
                return fromBackGroupCode(fd.getBackGroupCode());
            } else {
                return null;
            }
        }

    }
}

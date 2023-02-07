package com.zhongzhi.empty.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @author liuh
 * @date 2021年10月30日
 */
public interface MobileReportGroupEnum {

    /**
     * 空号文件检测 未知1级:大数据有返回状态，但数据库没有匹配 未知2级：大数据没有返回状态
     */
    enum FileDetection implements MobileReportGroupEnum {
        //实号1组
        REAL("real", "1"),
        //空号1组
        EMPTY("empty", "0"),
        //沉默
        SILENCE("silence", "4"),
        //停机
        OUT_SERVICE("out_service", "2"),
        //关机
        SHUT("shut", "5"),
        //状态不能识别
        UNKNOWN("unknown", "3"),
        //没有检测结果
        NO_RESULT("no_result", "-1"),;
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

    /**
     * 空号api检测
     */
    enum ApiDetection implements MobileReportGroupEnum {
        //空号
        EMPTY("empty", "0"),
        //实号
        REAL("real", "1"),
        //停机
        OUT_SERVICE("out_service", "2"),
        //状态不能识别
        UNKNOWN("unknown", "3"),
        //沉默
        SILENCE("silence", "4"),
        //关机
        SHUT("shut", "5"),
        //没有检测结果
        NO_RESULT("no_result", "-1"),;
        private String typeCode;
        private String apiCode;


        ApiDetection(String typeCode, String apiCode) {
            this.typeCode = typeCode;
            this.apiCode = apiCode;
        }


        public String getTypeCode() {
            return typeCode;
        }


        public String getApiCode() {
            return apiCode;
        }


        /**
         * 根据组别号返回枚举
         *
         * @param typeCode
         * @return
         */
        public static ApiDetection fromTypeCode(String typeCode) {
            for (ApiDetection apiDetection : ApiDetection.values()) {
                if (apiDetection.getTypeCode().equals(typeCode)) {
                    return apiDetection;
                }
            }
            return null;
        }

    }

}

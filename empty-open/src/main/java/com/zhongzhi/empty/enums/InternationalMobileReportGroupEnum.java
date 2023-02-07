package com.zhongzhi.empty.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * @author liuh
 * @date 2022年9月13日
 */
public interface InternationalMobileReportGroupEnum {

    enum FileDetection implements InternationalMobileReportGroupEnum {
        //激活
        ACTIVATE("activate", "","blue","激活.txt"),
        //未激活
        NO_ACTIVE("noactive", "","gray","未激活.txt"),
        //未知
        UNKNOWN("unknown", "","yellow","未知.txt"),
        //没有检测结果
        NO_RESULT("no_result", "","",""),;
        /**
         * 组别号
         */
        private String groupCode;
        /**
         * 后置组别号
         */
        private String backGroupCode;
        
        /**
         * 状态颜色
         */
        private String color;
        
        /**
         * 结果包名称
         */
        private String txtSuffix;


        FileDetection(String groupCode, String backGroupCode,String color,String txtSuffix) {
            this.groupCode = groupCode;
            this.backGroupCode = backGroupCode;
            this.setColor(color);
            this.setTxtSuffix(txtSuffix);
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

        public static String getColor(String group){
    		String color = "";
    		for (FileDetection fileDetection : FileDetection.values()) {
    			if (fileDetection.getGroupCode().equals(group)) {
    				color = fileDetection.getColor();
    				break;
    			}
    		}
    		return color;
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


		public String getColor() {
			return color;
		}


		public void setColor(String color) {
			this.color = color;
		}


		public String getTxtSuffix() {
			return txtSuffix;
		}


		public void setTxtSuffix(String txtSuffix) {
			this.txtSuffix = txtSuffix;
		}

    }

    /**
     * api检测
     */
    enum ApiDetection implements InternationalMobileReportGroupEnum {
        //激活
    	ACTIVATE("activate", "1"),
        //未激活
    	NO_ACTIVE("noactive", "2"),
        //未知
    	UNKNOWN("unknown", "0"),
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

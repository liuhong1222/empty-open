package com.zhongzhi.empty.service.direct;

import lombok.Data;

import java.io.Serializable;

/**
 * 定向国际检测进度任务
 * @author liuh
 * @date 2022年10月18日
 */
@Data
public class ProgressTaskInfo implements Serializable {
	
	private static final long serialVersionUID = 6034881331119651207L;

	/**
	 * 用户id
	 */
    private Long customerId;
    
    /**
	 * 定向国际检测记录id
	 */
    private Long intDirectId;
    
    /**
	 *国码
	 */
    private String countryCode;
    
    /**
	 * 产品类型
	 */
    private String productType;
    
    /**
	 * 外部文件id
	 */
    private String externFileId;
    
    /**
     * 文件地址
     */
    private String fileUrl;
    
    /**
     * 源文件名称
     */
    private String sourceFileName;
    
    /**
     * 文件源号码个数
     */
    private Integer mobileCount;
}

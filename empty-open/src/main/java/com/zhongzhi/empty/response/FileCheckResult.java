package com.zhongzhi.empty.response;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 空号文件检测返回结果实体类
 * @author liuh
 * @date 2021年10月27日
 */
@Data
@ApiModel("空号文件检测返回结果实体类")
public class FileCheckResult implements Serializable {

	private static final long serialVersionUID = 8102409932135305292L;

	private Integer status;

    private Long sendId;
    
    private String zip_url;
    
    private String active_url;
    
    private String empty_url;
    
    private String risk_url;
    
    private String silent_url;
}

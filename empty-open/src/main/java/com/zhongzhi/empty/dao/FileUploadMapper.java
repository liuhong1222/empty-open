package com.zhongzhi.empty.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zhongzhi.empty.entity.FileUpload;

/**
 * @author liuh
 * @date 2021年11月4日
 */
@Mapper
public interface FileUploadMapper {

    int saveOne(FileUpload fileUpload);
    
    FileUpload findOne(@Param("id")Long id);
}

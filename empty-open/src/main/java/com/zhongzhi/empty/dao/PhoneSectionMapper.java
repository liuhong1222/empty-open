package com.zhongzhi.empty.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import com.zhongzhi.empty.entity.PhoneSection;

@Mapper
public interface PhoneSectionMapper {

	List<PhoneSection> findAll();
}

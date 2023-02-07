package com.zhongzhi.empty.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.zhongzhi.empty.dao.PhoneSectionMapper;
import com.zhongzhi.empty.entity.PhoneSection;

import lombok.extern.slf4j.Slf4j;

/**
 * 号码归属地实现表
 * @author liuh
 * @date 2021年11月4日
 */
@Slf4j
@Service
public class PhoneSectionService {

	@Autowired
	private PhoneSectionMapper phoneSectionMapper;
	
	private Map<String, PhoneSection> phoneSectionMap = new HashMap<String, PhoneSection>();
	
	public PhoneSection getPhoneSection(String section) {
		return phoneSectionMap.get(section);
	}
	
	@PostConstruct
	private void initPhoneSectionData() {
		List<PhoneSection> list = phoneSectionMapper.findAll();
		if(CollectionUtils.isEmpty(list)) {
			log.error("初始化号码归属地数据失败，查无数据");
			return;
		}
		
		phoneSectionMap = list.stream().collect(Collectors.toMap(PhoneSection::getSection, Function.identity()));	
		log.info("初始化号码归属地数据成功, 初始化数据条数为：" + phoneSectionMap.size());
	}
}

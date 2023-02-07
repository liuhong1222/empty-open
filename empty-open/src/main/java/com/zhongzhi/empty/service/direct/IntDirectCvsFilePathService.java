package com.zhongzhi.empty.service.direct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.dao.IntDirectCvsFilePathMapper;
import com.zhongzhi.empty.entity.IntDirectCvsFilePath;

import lombok.extern.slf4j.Slf4j;

/**
 * 定向国际检测记录文件实现类
 * @author liuh
 * @date 2022年10月18日
 */
@Slf4j
@Service
public class IntDirectCvsFilePathService {

	@Autowired
	private IntDirectCvsFilePathMapper intDirectCvsFilePathMapper;
	
	public int saveOne(IntDirectCvsFilePath intDirectCvsFilePath) {
		return intDirectCvsFilePathMapper.saveOne(intDirectCvsFilePath);
	}
	
	public int delete(Long id) {
		return intDirectCvsFilePathMapper.delete(id);
	}
	
	public IntDirectCvsFilePath findOne(Long customerId, Long intDirectId) {
		return intDirectCvsFilePathMapper.findOne(customerId, intDirectId);
	}
}

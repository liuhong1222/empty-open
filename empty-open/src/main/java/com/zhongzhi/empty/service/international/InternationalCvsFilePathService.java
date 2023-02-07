package com.zhongzhi.empty.service.international;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.dao.InternationalCvsFilePathMapper;
import com.zhongzhi.empty.entity.InternationalCvsFilePath;

import lombok.extern.slf4j.Slf4j;

/**
 * 国际文件检测记录实现类
 * @author liuh
 * @date 2022年6月9日
 */
@Slf4j
@Service
public class InternationalCvsFilePathService {

	@Autowired
	private InternationalCvsFilePathMapper internationalCvsFilePathMapper;
	
	public int saveOne(InternationalCvsFilePath internationalCvsFilePath) {
		return internationalCvsFilePathMapper.saveOne(internationalCvsFilePath);
	}
	
	public int delete(Long id) {
		return internationalCvsFilePathMapper.delete(id);
	}
	
	public InternationalCvsFilePath findOne(Long customerId, Long internationalId) {
		return internationalCvsFilePathMapper.findOne(customerId, internationalId);
	}
}

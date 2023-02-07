package com.zhongzhi.empty.service.empty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.dao.CvsFilePathMapper;
import com.zhongzhi.empty.entity.CvsFilePath;

import lombok.extern.slf4j.Slf4j;

/**
 * 空号在线检测结果包信息实现类
 * @author liuh
 * @date 2021年10月30日
 */
@Slf4j
@Service
public class CvsFilePathService {

	@Autowired
	private CvsFilePathMapper cvsFilePathMapper;
	
	public int saveOne(CvsFilePath cvsFilePath) {
		return cvsFilePathMapper.saveOne(cvsFilePath);
	}
	
	public CvsFilePath findOne(Long customerId,Long emptyId) {
		return cvsFilePathMapper.findOne(customerId,emptyId);
	}
}

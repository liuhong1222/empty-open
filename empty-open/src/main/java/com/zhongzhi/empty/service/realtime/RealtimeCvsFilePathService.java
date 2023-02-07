package com.zhongzhi.empty.service.realtime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zhongzhi.empty.dao.RealtimeCvsFilePathMapper;
import com.zhongzhi.empty.entity.RealtimeCvsFilePath;

import lombok.extern.slf4j.Slf4j;

/**
 * 实时在线检测结果包信息实现类
 * @author liuh
 * @date 2021年10月30日
 */
@Slf4j
@Service
public class RealtimeCvsFilePathService {

	@Autowired
	private RealtimeCvsFilePathMapper realtimeCvsFilePathMapper;
	
	public int saveOne(RealtimeCvsFilePath realtimeCvsFilePath) {
		return realtimeCvsFilePathMapper.saveOne(realtimeCvsFilePath);
	}
	
	public RealtimeCvsFilePath findOne(Long customerId,Long emptyId) {
		return realtimeCvsFilePathMapper.findOne(customerId,emptyId);
	}
}

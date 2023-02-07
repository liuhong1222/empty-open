package com.zhongzhi.empty.service.direct;

import com.zhongzhi.empty.dao.IntDirectCheckMapper;
import com.zhongzhi.empty.entity.IntDirectCheck;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

/**
 * 定向国际检测记录实现类
 * @author liuh
 * @date 2022年10月18日
 */
@Slf4j
@Service
public class IntDirectCheckService {

	@Autowired
	private IntDirectCheckMapper inteDirectCheckMapper;
	
	public IntDirectCheck findOne(Long customerId,Long intDirectId) {
		return inteDirectCheckMapper.findOne(customerId, intDirectId);
	}
	
	public int updateOne(IntDirectCheck intDirectCheck) {
		return inteDirectCheckMapper.updateOne(intDirectCheck);
	}
	
	public int saveOne(IntDirectCheck intDirectCheck) {
		return inteDirectCheckMapper.saveOne(intDirectCheck);
	}
	
	public int saveList(List<IntDirectCheck> list) {
		return inteDirectCheckMapper.saveList(list);
	}

	public List<IntDirectCheck> findByStatusAndCreateTime(int status, Date date) {
		return inteDirectCheckMapper.findByStatusAndCreateTime(status, date);
	}
}

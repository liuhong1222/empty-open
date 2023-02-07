package com.zhongzhi.empty.service.international;

import com.zhongzhi.empty.dao.InternationalCheckMapper;
import com.zhongzhi.empty.entity.InternationalCheck;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 国际号码检测记录实现类
 * @author liuh
 * @date 2022年6月8日
 */
@Slf4j
@Service
public class InternationalCheckService {

	@Autowired
	private InternationalCheckMapper internationalCheckMapper;
	
	public InternationalCheck findOne(Long customerId,Long internationalId) {
		return internationalCheckMapper.findOne(customerId, internationalId);
	}
	
	public int updateOne(InternationalCheck internationalCheck) {
		return internationalCheckMapper.updateOne(internationalCheck);
	}
	
	public int saveOne(InternationalCheck internationalCheck) {
		return internationalCheckMapper.saveOne(internationalCheck);
	}
	
	public int saveList(List<InternationalCheck> list) {
		return internationalCheckMapper.saveList(list);
	}
}

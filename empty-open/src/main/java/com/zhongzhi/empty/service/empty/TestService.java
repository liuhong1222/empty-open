package com.zhongzhi.empty.service.empty;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.http.unn.UnnData;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.response.UnnMobileNewStatus;
import com.zhongzhi.empty.service.gateway.UnnService;
import lombok.extern.slf4j.Slf4j;

/**
 * 空号检测系列接口实现类
 * @author liuh
 * @date 2021年10月27日
 */
@Slf4j
@Service
public class TestService {
	
	@Autowired
	private UnnService unnService;
	
	public ApiResult<List<UnnMobileNewStatus>> batchCheckNew(String[] mobiles,String ip){
		try {
			// 执行空号检测
	        List<UnnData> list = unnService.emptyCheckNew( mobiles);
	        if(CollectionUtils.isEmpty(list)) {
	            return ApiResult.result(ApiCode.FAIL, "系统异常，空号检测结果为空", null);
	        }
	       
	        return ApiResult.ok(list);
		} catch (Exception e) {
			log.error("",e);
			return ApiResult.result(ApiCode.FAIL, "系统异常", null);
		}
	}
}

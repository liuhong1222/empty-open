package com.zhongzhi.empty.controller;

import com.zhongzhi.empty.enums.ApiCode;
import com.zhongzhi.empty.response.ApiResult;
import com.zhongzhi.empty.service.empty.FileEmptyCheckService;
import com.zhongzhi.empty.service.file.FileService;
import com.zhongzhi.empty.service.international.FileInternationalCheckNewService;
import com.zhongzhi.empty.service.international.InternationalFileService;
import com.zhongzhi.empty.service.realtime.FileRealtimeCheckService;
import com.zhongzhi.empty.service.realtime.RealtimeFileService;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

/**
 * 官网接口
 * @author liuh
 * @date 2021年11月3日
 */
@Slf4j
@RestController
@RequestMapping("/data/api")
@Api("官网接口 ")
public class OfficialApiController extends BaseController{
	
	@Autowired
	private FileEmptyCheckService fileEmptyCheckService;
	
	@Autowired
	private FileRealtimeCheckService fileRealtimeCheckService;
	
	@Autowired
	private FileService fileService;
	
	@Autowired
	private RealtimeFileService realtimeFileService;
	
	@Autowired
	private FileInternationalCheckNewService fileInternationalCheckNewService;
	
	@Autowired
	private InternationalFileService internationalFileService;

    @PostMapping("/executeEmptyCheck")
    public ApiResult executeEmptyCheck(HttpServletRequest request,
											    		@RequestParam("customerId") Long customerId,
											    		@RequestParam("emptyId") Long emptyId,
											    		@RequestParam("totalNumber") Long totalNumber,
											    		@RequestParam("sourceFileName") String sourceFileName,
											    		@RequestParam("uploadPath") String uploadPath) {
    	if(customerId == null) {
            return ApiResult.result(ApiCode.FAIL, "customerId不能为空", null);
    	}
    	
    	if(emptyId == null) {
            return ApiResult.result(ApiCode.FAIL, "emptyId不能为空", null);
    	}
    	
    	if(totalNumber == null) {
            return ApiResult.result(ApiCode.FAIL, "totalNumber不能为空", null);
    	}
    	
    	if(StringUtils.isBlank(sourceFileName)) {
            return ApiResult.result(ApiCode.FAIL, "sourceFileName不能为空", null);
    	}
    	
    	if(StringUtils.isBlank(uploadPath)) {
            return ApiResult.result(ApiCode.FAIL, "uploadPath不能为空", null);
    	}
    	
    	log.info("{},官网发起空号在线检测,emptyId:{},totalNumber:{},sourceFileName:{},uploadPath:{}",
    			customerId, emptyId, totalNumber, sourceFileName, uploadPath);
        return fileEmptyCheckService.executeEmptyCheck(customerId, emptyId, totalNumber, sourceFileName, uploadPath);
    }
    
    @PostMapping("/executeRealtimeCheck")
    public ApiResult executeRealtimeCheck(HttpServletRequest request,
    												@RequestParam("customerId") Long customerId,
										    		@RequestParam("realtimeId") Long realtimeId,
										    		@RequestParam("totalNumber") Long totalNumber,
										    		@RequestParam("sourceFileName") String sourceFileName,
										    		@RequestParam("uploadPath") String uploadPath) {
    	if(customerId == null) {
            return ApiResult.result(ApiCode.FAIL, "customerId不能为空", null);
    	}
    	
    	if(realtimeId == null) {
            return ApiResult.result(ApiCode.FAIL, "realtimeId不能为空", null);
    	}
    	
    	if(totalNumber == null) {
            return ApiResult.result(ApiCode.FAIL, "totalNumber不能为空", null);
    	}
    	
    	if(StringUtils.isBlank(sourceFileName)) {
            return ApiResult.result(ApiCode.FAIL, "sourceFileName不能为空", null);
    	}
    	
    	if(StringUtils.isBlank(uploadPath)) {
            return ApiResult.result(ApiCode.FAIL, "uploadPath不能为空", null);
    	}
    	
    	log.info("{},官网发起实时在线检测,realtimeId:{},totalNumber:{},sourceFileName:{},uploadPath:{}",
    			customerId, realtimeId, totalNumber, sourceFileName, uploadPath);
    	return fileRealtimeCheckService.executeRealtimeCheck(customerId, realtimeId, totalNumber, sourceFileName, uploadPath);
    }
    
    @PostMapping("/executeInternationalCheck")
    public ApiResult executeInternationalCheck(HttpServletRequest request,
    												@RequestParam("customerId") Long customerId,
										    		@RequestParam("internationalId") Long internationalId,
										    		@RequestParam("totalNumber") Long totalNumber,
										    		@RequestParam("sourceFileName") String sourceFileName,
										    		@RequestParam("uploadPath") String uploadPath,
										    		@RequestParam("countryCode") String countryCode,
										    		@RequestParam("productType") String productType) {
    	if(customerId == null) {
            return ApiResult.result(ApiCode.FAIL, "customerId不能为空", null);
    	}
    	
    	if(internationalId == null) {
            return ApiResult.result(ApiCode.FAIL, "internationalId不能为空", null);
    	}
    	
    	if(totalNumber == null) {
            return ApiResult.result(ApiCode.FAIL, "totalNumber不能为空", null);
    	}
    	
    	if(StringUtils.isBlank(sourceFileName)) {
            return ApiResult.result(ApiCode.FAIL, "sourceFileName不能为空", null);
    	}
    	
    	if(StringUtils.isBlank(uploadPath)) {
            return ApiResult.result(ApiCode.FAIL, "uploadPath不能为空", null);
    	}
    	
    	log.info("{},官网发起国际在线检测,internationalId:{},totalNumber:{},sourceFileName:{},uploadPath:{},countryCode:{}",
    			customerId, internationalId, totalNumber, sourceFileName, uploadPath,countryCode);
    	return fileInternationalCheckNewService.executeInternationalCheck(customerId, internationalId, totalNumber, 
    			sourceFileName, uploadPath, countryCode,productType);
    }
    
    @PostMapping("/deleteTempFileByEmpty")
    public ApiResult deleteTempFileByEmpty(HttpServletRequest request,@RequestParam("fileUrl") String fileUrl) {
    	fileService.deleteTempFileByEnd(fileUrl);
    	return ApiResult.ok();
    }
    
    @PostMapping("/deleteTempFileByRealtime")
    public ApiResult deleteTempFileByRealtime(HttpServletRequest request,@RequestParam("fileUrl") String fileUrl) {
    	realtimeFileService.deleteTempFileByEnd(fileUrl);
    	return ApiResult.ok();
    }
    
    @PostMapping("/deleteTempFileByInternational")
    public ApiResult deleteTempFileByInternational(HttpServletRequest request,@RequestParam("fileUrl") String fileUrl) {
    	internationalFileService.deleteTempFileByEnd(fileUrl);
    	return ApiResult.ok();
    }
    
    @PostMapping("/realtimeCheckBySingle")
    public ApiResult realtimeCheckBySingle(HttpServletRequest request,@RequestParam("customerId") Long customerId
    		,@RequestParam("mobile") String mobile) {
    	if(customerId == null) {
            return ApiResult.result(ApiCode.FAIL, "customerId不能为空", null);
    	}
    	
    	if(StringUtils.isBlank(mobile)) {
            return ApiResult.result(ApiCode.FAIL, "mobile不能为空", null);
    	}
    	
    	return realtimeFileService.realtimeCheckBySingle(customerId,mobile,super.getIpAddr(request));
    }
}


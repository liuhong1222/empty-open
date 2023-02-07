package com.zhongzhi.empty.service.file;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.constants.RedisKeyConstant;
import com.zhongzhi.empty.dao.FileUploadMapper;
import com.zhongzhi.empty.entity.FileUpload;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.util.DingDingMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件上传表实现类
 * @author liuh
 * @date 2021年11月4日
 */
@Slf4j
@Service
public class FileUploadService {

	@Autowired
	private FileUploadMapper fileUploadMapper;
	
	@Autowired
	private DingDingMessage dingDingMessage;
	
	@Autowired
	private RedisClient redisClient;
	
	private static ExecutorService executorService = Executors.newFixedThreadPool(10);
	
	public void saveOne(Long emptyId,Long customerId,String sourceFileName,String filePath,int fileRows,String fileMd5) {
		executorService.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					FileUpload fileUpload = new FileUpload();
					fileUpload.setId(emptyId);
					fileUpload.setCustomerId(customerId);
					fileUpload.setFileName(sourceFileName.substring(0,sourceFileName.lastIndexOf(".")));
					fileUpload.setFileRows(fileRows);
					fileUpload.setFileUploadUrl(filePath.replace(".xls", ".txt"));
					fileUpload.setFileMd5(fileMd5);
					fileUpload.setFileSize(String.valueOf(new File(filePath).length()));
					int counts = saveOne(fileUpload);
					if(counts != 1) {
						log.error("{}, api文件空号检测保存上传记录失败，info:{}",customerId,JSON.toJSONString(fileUpload));
						dingDingMessage.sendMessage(String.format("警告：%s, api文件空号检测保存上传记录失败，emptyId:%s", customerId,emptyId));
						return ;
					}
					
					redisClient.set(String.format(RedisKeyConstant.FILE_MD5_CACHE_KEY, customerId,fileUpload.getFileMd5()), fileUpload.getFileName(), 3600 * 24);
					log.info("{}, api文件空号检测保存上传记录成功，info:{}",customerId,JSON.toJSONString(fileUpload));
				} catch (Exception e) {
					log.error("{}, api文件空号检测保存上传记录异常，emptyId:{},filePath:{},sourceFileName:{}",customerId,emptyId,filePath,sourceFileName);
					dingDingMessage.sendMessage(String.format("警告：%s, api文件空号检测保存上传记录异常，emptyId:%s", customerId,emptyId));
				}
			}
		});
	}
	
	public int saveOne(FileUpload fileUpload) {
		return fileUploadMapper.saveOne(fileUpload);
	}
	
	public FileUpload findOne(Long id) {
		return fileUploadMapper.findOne(id);
	}
	
	public void handleFileMd5Cache(Long fileId,Long customerId) {
		FileUpload fileUpload = findOne(fileId);
		if(fileUpload == null) {
			return ;
		}
		
		redisClient.set(String.format(RedisKeyConstant.FILE_MD5_CACHE_KEY, customerId,fileUpload.getFileMd5()), fileUpload.getFileName(), 3600 * 24);
	}
}

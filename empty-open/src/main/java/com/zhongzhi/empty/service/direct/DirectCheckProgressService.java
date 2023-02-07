package com.zhongzhi.empty.service.direct;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.constants.IntDirectRedisKeyConstant;
import com.zhongzhi.empty.redis.RedisClient;
import com.zhongzhi.empty.util.DingDingMessage;

import lombok.extern.slf4j.Slf4j;

/**
 * 定向国际检测进度实现类
 * @author liuh
 * @date 2022年10月18日
 */
@Slf4j
@Service
public class DirectCheckProgressService {
	
	private Map<ScheduledFuture<?>, ProgressTaskInfo> map = new ConcurrentHashMap<ScheduledFuture<?>, ProgressTaskInfo>();
	
	private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

	@Autowired
    private DingDingMessage dingDingMessage;
	
	@Autowired
	private FileInternationalCheckService fileInternationalCheckService;
	
	@Autowired
	private RedisClient redisClient;
	
	@PostConstruct
	private void timingCancelTask() {
		// 用于取消定向国际检测进度查询定时任务
		scheduledExecutorService.scheduleWithFixedDelay(new TimingCancelTask(), 15, 10, TimeUnit.SECONDS);
	}
	
	public Boolean timingExecute(ProgressTaskInfo taskInfo) {
		// 首次延时6s执行，之后执行完成每隔5s执行
		ScheduledFuture<?> future1 = scheduledExecutorService.scheduleWithFixedDelay(new TimingTask(taskInfo), 6, 5, TimeUnit.SECONDS);
		map.put(future1, taskInfo);
		return true;
	}
	
	//定时任务
    private class TimingTask implements Runnable {
    	
    	private ProgressTaskInfo progressTaskInfo;
    	
    	public TimingTask() {}
    	
    	public TimingTask(ProgressTaskInfo progressTaskInfo) {
    		this.progressTaskInfo = progressTaskInfo;
    	}
    	
		@Override
		public void run() {
			try {
				fileInternationalCheckService.queryIntDirectProcess(progressTaskInfo);
			} catch (Exception e) {
				log.error("执行定向国际检测进度查询定时任务异常，param:{},info:",JSON.toJSONString(progressTaskInfo),e);
	    		dingDingMessage.sendMessage(String.format("警告：%s,执行定向国际检测进度查询定时任务异常，intDirectId:%s，info:%s", progressTaskInfo.getCustomerId(),progressTaskInfo.getIntDirectId(),e));
			}
			
		}
    }
    
    private class TimingCancelTask implements Runnable {

		@Override
		public void run() {
			cancelTaskHandle();
		}
    	
    }
    
    private void cancelTaskHandle() {
    	try {
    		if(MapUtils.isEmpty(map)) {
        		return ;
        	}
        	
    		Iterator<ScheduledFuture<?>> it = map.keySet().iterator();
    		while(it.hasNext()) {
    			ScheduledFuture<?> future = it.next();
    			ProgressTaskInfo progressTaskInfo = map.get(future);
        		String fileTestCode = redisClient.get(String.format(IntDirectRedisKeyConstant.THE_RUN_KEY, progressTaskInfo.getCustomerId(), progressTaskInfo.getIntDirectId()));
        		if(CommonConstant.FILE_TEST_FAILED_CODE.equals(fileTestCode)) {
        			// 取消定时任务
        			future.cancel(true);
        			map.remove(future);
        			log.info("取消定向国际检测进度查询任务成功，info:{}",JSON.toJSONString(progressTaskInfo));
        		}
    		}
		} catch (Exception e) {
			log.error("执行取消定向国际检测进度查询定时任务异常，map:{},info:",JSON.toJSONString(map),e);
    		dingDingMessage.sendMessage(String.format("警告：执行取消定向国际检测进度查询定时任务异常, e:%s",e));
		}
    }
}

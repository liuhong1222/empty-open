package com.zhongzhi.empty.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.mongodb.bulk.BulkWriteResult;
import com.zhongzhi.empty.constants.CommonConstant;
import com.zhongzhi.empty.entity.MobileStatusCache;
import com.zhongzhi.empty.response.UnnMobileStatus;
import lombok.extern.slf4j.Slf4j;


/**
 * 本地缓存空号检测实现类
 * @author liuh
 * @date 2021年10月28日
 */
@Slf4j
@Service
public class LocalCacheService {
	
	@Autowired
    private MongoTemplate mongoTemplate;
	
	private static ExecutorService executorService = Executors.newFixedThreadPool(10);
	
	public List<UnnMobileStatus> emptyCheck(Long customerId,String[] mobiles){
		try {
			Query query = new Query();
	        query.addCriteria(Criteria.where("_id").in(Arrays.asList(mobiles)));
	        List<MobileStatusCache> templist = mongoTemplate.find(query, MobileStatusCache.class);
	        if (CollectionUtils.isEmpty(templist)) {
	        	log.error("{}, 查询空号检测本地缓存库失败，查无数据",customerId);
				return null;
			}
	        
	        List<UnnMobileStatus> list = new ArrayList<UnnMobileStatus>();
	        for(MobileStatusCache mobileStatusCache : templist) {
	        	UnnMobileStatus unnMobileStatus = new UnnMobileStatus();
	        	unnMobileStatus.setMobile(mobileStatusCache.getMobile());
	        	unnMobileStatus.setChargesStatus(CommonConstant.MOBILE_CHARGE_STATUS);
	        	unnMobileStatus.setStatus(mobileStatusCache.getStatus());        	
	        	list.add(unnMobileStatus);
	        }
	        
	        log.info("{}, 查询空号检测本地缓存库成功，号码个数：{}",customerId,list.size());
			return list;
		} catch (Exception e) {
			log.error("{}, 查询空号检测本地缓存库异常, info:",customerId,e);
			return null;
		}
	}
	
	public void saveList(List<MobileStatusCache> list) {
		executorService.execute(new Runnable() {
			
			@Override
			public void run() {
				if(CollectionUtils.isEmpty(list)) {
					return ;
				}
				
				try {
					Map<String, String> map = list.stream().collect(Collectors.toMap(MobileStatusCache::getMobile,MobileStatusCache::getStatus));
					List<MobileStatusCache> resultList = new ArrayList<MobileStatusCache>();
					List<Query> queryList = new ArrayList<Query>();
					for(String key:map.keySet()) {
						MobileStatusCache mobileStatusCache = new MobileStatusCache();
						mobileStatusCache.setMobile(key);
						mobileStatusCache.setStatus(map.get(key));
						mobileStatusCache.setCreateTime(new Date());
						resultList.add(mobileStatusCache);
						queryList.add(new Query(new Criteria("_id").is(key)));
					}
										
					BulkOperations operation = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, MobileStatusCache.class);
					operation.remove(new ArrayList<Query>(queryList));//批量删除
					operation.insert(resultList);//批量插入
					BulkWriteResult bulkWriteResult = operation.execute();
					log.info("批量插入号码池成功，remove:{},insert:{}", bulkWriteResult.getDeletedCount(),bulkWriteResult.getInsertedCount());
				} catch (Exception e) {
					log.error("批量插入号码池异常，count:{},list:{}, info:", list.size(), JSON.toJSONString(list), e);
				}
			}
		});
	}
}

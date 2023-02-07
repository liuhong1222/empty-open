package com.zhongzhi.empty.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * mongodb配置
 * @author liuh
 * @date 2021年10月28日
 */
@Configuration
public class SpringMongoConfig extends AbstractMongoConfiguration {

	public static ServerAddress seed1 = new ServerAddress("dds-bp137becb7ad0db41.mongodb.rds.aliyuncs.com", 3717);
	public static ServerAddress seed2 = new ServerAddress("dds-bp137becb7ad0db42.mongodb.rds.aliyuncs.com", 3717);
	public static String username = "credit_zxm";
	public static String password = "gB3Yup!aIkW80cSQ";
	public static String ReplSetName = "mgset-57091040";
	public static String DEFAULT_DB = "credit";

	@Value("${mongodburi}")
	String mongodburi;
	
	@Value("${mongodbEnvironment}")
	String mongodbEnvironment;

	@Override
	protected String getDatabaseName() {
		return DEFAULT_DB;
	}

	@Override
	@Bean
	public MongoClient mongoClient() {

		if (!mongodbEnvironment.equals("produced")) {
			MongoClientURI mongoClientURI = new MongoClientURI(mongodburi);  
	        return new MongoClient(mongoClientURI);  
		}
		
		// 构建Seed列表
		List<ServerAddress> seedList = new ArrayList<ServerAddress>();
		seedList.add(seed1);
		seedList.add(seed2);
		// 构建鉴权信息
		List<MongoCredential> credentials = new ArrayList<MongoCredential>();
		credentials.add(MongoCredential.createScramSha1Credential(username, DEFAULT_DB, password.toCharArray()));
		// 构建操作选项，requiredReplicaSetName属性外的选项根据自己的实际需求配置，默认参数满足大多数场景
		MongoClientOptions options = MongoClientOptions.builder().requiredReplicaSetName(ReplSetName)
				.socketTimeout(10000).connectionsPerHost(1500) // 最大连接数
				.minConnectionsPerHost(0) // 最小连接数
				.maxWaitTime(120000) // 最大等待可用连接的时间
				.connectTimeout(10000) // 连接超时时间
				.build();

		return new MongoClient(seedList, credentials, options);

	}

	// 去掉_class
	@Override
	@Bean
	public MongoTemplate mongoTemplate() throws Exception {
		MongoDbFactory factory = mongoDbFactory();

		MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(factory),
				new MongoMappingContext());
		converter.setTypeMapper(new DefaultMongoTypeMapper(null));

		return new MongoTemplate(factory, converter);
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
		return new PropertySourcesPlaceholderConfigurer();
	}
}

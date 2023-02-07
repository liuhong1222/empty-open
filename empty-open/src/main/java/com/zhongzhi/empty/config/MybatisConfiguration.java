package com.zhongzhi.empty.config;

import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

import org.springframework.transaction.support.TransactionTemplate;

import com.github.pagehelper.PageInterceptor;

@Configuration
@AutoConfigureAfter(DatasourceConfig.class)
@ConfigurationProperties
@EnableTransactionManagement
@MapperScan(basePackages="com.zhongzhi.empty.dao",sqlSessionFactoryRef = "sqlSessionFactory")
public class MybatisConfiguration implements TransactionManagementConfigurer {

    private Logger logger = LoggerFactory.getLogger(DatasourceConfig.class);

    //  配置类型别名
    @Value("${mybatis.type-aliases-package}")
    private String typeAliasesPackage;
    
    //  加载全局的配置文件
    @Value("${mybatis.config-location}")
    private String configLocation;
    
    @Value("${mybatis.mapper.resource}")
    private String mapperResource;

    @Autowired
    private DataSource dataSource;

    // 提供SqlSeesion
    @Bean(name = "sqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory() {
        try {
            SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
            sessionFactoryBean.setDataSource(dataSource);

            // 读取配置
            sessionFactoryBean.setTypeAliasesPackage(typeAliasesPackage);

            //设置mybatis-config.xml配置文件位置
            sessionFactoryBean.setConfigLocation(new DefaultResourceLoader().getResource(configLocation));
            sessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperResource));
            //添加分页插件、打印sql插件
            Interceptor[] plugins = new Interceptor[]{pageInterceptor()};
            sessionFactoryBean.setPlugins(plugins);

            return sessionFactoryBean.getObject();
        } catch (IOException e) {
            logger.error("mybatis resolver mapper*xml is error",e);
            return null;
        } catch (Exception e) {
            logger.error("mybatis sqlSessionFactoryBean create error",e);
            return null;
        }
    }

    @Bean("sqlSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    //事务管理
    @Bean("platformTransactionManager")
    @Primary
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @Primary
    public TransactionTemplate transactionTemplate(@Qualifier("platformTransactionManager")  PlatformTransactionManager platformTransactionManager){
        return new TransactionTemplate(platformTransactionManager);
    }

    private PageInterceptor pageInterceptor() {
        PageInterceptor pi = new PageInterceptor();
        Properties p = new Properties();
        p.setProperty("dialect", "com.github.pagehelper.PageHelper");
        p.setProperty("offsetAsPageNum", "true");
        p.setProperty("rowBoundsWithCount", "true");
        p.setProperty("reasonable", "true");
        p.setProperty("returnPageInfo", "check");
        p.setProperty("params", "count=countSql");
        pi.setProperties(p);
        return pi;
    }
}

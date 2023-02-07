package com.zhongzhi.empty.config;


import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import com.zhongzhi.empty.util.Snowflake;

/**
 * 雪花算法配置
 * @author liuh
 * @date 2021年10月26日
 */
@Configurable
public class SysConfiguration {

    @Value("${sys.work-id:0}")
    private Long workId;

    @Value("${sys.data-center-id:0}")
    private Long dataCenterId;

    @Bean(name="snowflake")
    public Snowflake snowflake(){
        return new Snowflake(workId,dataCenterId);
    }
}

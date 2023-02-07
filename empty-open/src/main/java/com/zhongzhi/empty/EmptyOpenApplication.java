package com.zhongzhi.empty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.zhongzhi.empty.config.SysConfiguration;

@SpringBootApplication
@Import(SysConfiguration.class)
public class EmptyOpenApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmptyOpenApplication.class, args);
	}

}

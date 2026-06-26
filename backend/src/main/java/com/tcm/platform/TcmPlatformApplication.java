package com.tcm.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 中医问诊与养生平台 - 启动类
 * 
 * 任务：
 * 1. 添加 @SpringBootApplication 注解（已添加）
 * 2. 添加 @MapperScan 注解，扫描 Mapper 接口所在的包
 *    提示：Mapper 接口位于 com.tcm.platform.mapper 包下
 * 3. 完善 main 方法，启动 Spring Boot 应用
 */
@SpringBootApplication
@MapperScan("com.tcm.platform.mapper")
public class TcmPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcmPlatformApplication.class, args);
    }
}

package com.softwarego.selection;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = "com.softwarego.selection", annotationClass = Mapper.class)
@EnableScheduling
public class SelectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SelectionApplication.class, args);
    }
}

package com.monitor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@org.springframework.boot.autoconfigure.SpringBootApplication
@ComponentScan(basePackages = "com.monitor")
@EnableScheduling
@EnableAsync
@EnableJpaRepositories
@Slf4j
public class SpringBootApplication {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApplication.class, args);

//        ApplicationContext ctx = SpringApplication.run(SpringBootApplication.class, args);
//        //所有的bean,参考：http://412887952-qq-com.iteye.com/blog/2314051
//        String[] beanNames = ctx.getBeanDefinitionNames();
//        //String[] beanNames = ctx.getBeanNamesForAnnotation(RestController.class);//所有添加该注解的bean
//        int i = 0;
//        for (String str : beanNames) {
//            log.info("{},beanName:{},{}", ++i, str, ctx.getBean(str) == null);
//        }
    }
}

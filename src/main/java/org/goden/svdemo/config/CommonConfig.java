package org.goden.svdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {

    // @Bean可将maven中的jar包内的Test对象注入IOC容器
    // 前提是要在Config中配置 @Configuration 或 @SpringBootApplication 注解下进行@Bean注入
    // 创建的对象默认名字是方法名即 testObject  若 @Bean("objectName") 则名字为:objectName
    // 方法内部需要注入IOC容器中已存在的Bean对象则只需要在方法中声明即可 如: public Test testObject(GgBond b) 声明对象GgBond
    // 如CommonConfig不在 @SpringBootApplication 扫描范围内 可在@SpringBootApplication 下使用@Import(CommonConfig.class) 注解使CommonConfig被扫描到
//    @Bean
//    public Test testObject(){
//        return new Test();
//    }
}

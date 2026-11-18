package org.goden.svdemo.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 1.创建mybatis plus拦截器,统一管理和注册多种内置插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 2.创建分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 3.设置分页插件的最大分页数
        // paginationInnerInterceptor.setMaxLimit(1000L);
        // 4.将分页插件添加到拦截器中
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }

}

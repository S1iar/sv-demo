package org.sliar.com.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.Bean;
import javax.sql.DataSource;
import java.util.List;

@AutoConfiguration
public class myBatisAutoConfig {

    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource) {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        return sqlSessionFactoryBean;
    }

    @Bean
    public MapperScannerConfigurer mapperScannerConfigurer(BeanFactory beanFactory) {
        // 扫描的包:启动类所在的包及其子包
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        List<String> packages = AutoConfigurationPackages.get(beanFactory);
        String basePackage = packages.get(0);
        mapperScannerConfigurer.setBasePackage(basePackage);
        // 扫描的注解
        mapperScannerConfigurer.setAnnotationClass(Mapper.class);
        return mapperScannerConfigurer;
    }
}

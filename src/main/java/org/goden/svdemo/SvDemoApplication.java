package org.goden.svdemo;

import org.goden.svdemo.config.CommonImportSelector;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

//@Import(CommonImportSelector.class)
@SpringBootApplication
public class SvDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SvDemoApplication.class, args);
	}

}

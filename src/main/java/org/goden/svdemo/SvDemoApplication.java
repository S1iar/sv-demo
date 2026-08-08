package org.goden.svdemo;

import org.goden.svdemo.anno.EnableCommonConfig;
import org.goden.svdemo.entity.Province;
import org.goden.svdemo.entity.YmlTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@EnableCommonConfig
@SpringBootApplication
public class SvDemoApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext run = SpringApplication.run(SvDemoApplication.class, args);

//		YmlTest ymlTest = run.getBean(YmlTest.class);
//		System.out.println(ymlTest);
//
//		Province province = run.getBean(Province.class);
//		System.out.println(province);
	}

}

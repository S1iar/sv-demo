package org.goden.svdemo;

import org.goden.svdemo.anno.EnableCommonConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@EnableCommonConfig
@SpringBootApplication
public class SvDemoApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext run = SpringApplication.run(SvDemoApplication.class, args);
	}

}

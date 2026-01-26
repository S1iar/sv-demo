package org.goden.svdemo;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class SvDemoApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void getToken(){
		Map<String, Object> claims = new HashMap<>();
		claims.put("id", "1");
		claims.put("username", "test");

		String token  = JWT.create()
				.withClaim("user", claims)
				.withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 30))
				.sign(Algorithm.HMAC256("demo"));

		System.out.println("token内容:"+token);
	}

}

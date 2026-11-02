package org.goden.svdemo;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.goden.svdemo.mapper.UserMapper;
import org.goden.svdemo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class SvDemoApplicationTests {

	@Autowired
	UserService userService;

	@Autowired
	UserMapper userMapper;


	@Test
	void generateToken(){
		Map<String, Object> claims = new HashMap<>();
		claims.put("id", "1");
		claims.put("username", "test");

		String token  = JWT.create()
				.withClaim("user", claims)
				.withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 30))
				.sign(Algorithm.HMAC256("demo"));

		System.out.println("token内容:"+token);
	}

	static class hsmDemo<K,V>{
		class Entry<K,V>{
			K key;
			V value;
			Entry<K,V> next;
			Entry(K key, V value){
				this.key = key;
				this.value = value;
			}
		}

		private int len = 16;
		private Entry<K,V>[] entrys;

		public hsmDemo(){
			entrys = new Entry[len];
		}

		public void put(K key, V value){
			int idx = Math.abs(key.hashCode()) % len;
			Entry<K,V> entry = entrys[idx];
			for(Entry<K,V> i = entry; i != null; i = i.next){
				if(i.key.equals(key)){
					i.value = value;
					return;
				}
			}
			Entry<K,V> newEntry = new Entry<>(key, value);
			newEntry.next = entry;
			entrys[idx] = newEntry;
		}

		public V get(K key){
			int idx = Math.abs(key.hashCode()) % len;
			Entry<K,V> entry = entrys[idx];
			for(Entry<K,V> i = entry; i != null; i = i.next){
				if(i.key.equals(key)){
					return i.value;
				}
			}
			return null;
		}
	}

}

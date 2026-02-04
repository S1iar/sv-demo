package org.goden.svdemo.service.impl;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import org.goden.svdemo.service.PassWordService;
import org.springframework.stereotype.Service;

@Service
public class PassWordServiceImpl implements PassWordService {

    // 特别注意：密钥必须是16字节（128位）
    // 示例密钥，生产环境务必从安全的环境变量或配置中心获取，不要硬编码！
    private static final byte[] keys = "svDemoKeysOnHere".getBytes(); // 16位密钥

    private static final SM4 sm4 = SmUtil.sm4(keys);
    public static final String SM4_PREFIX = "@SM4@-"; // 加密值前缀

    //加密
    @Override
    public String encodePassword(String rawPassword) {
        return SM4_PREFIX + sm4.encryptBase64(rawPassword);
    }

    //解密
    @Override
    public String matches(String encodedPassword) {
        if (encodedPassword != null && encodedPassword.startsWith(SM4_PREFIX)) {
            return sm4.decryptStr(encodedPassword.substring(SM4_PREFIX.length()));
        }
        return encodedPassword; // 如果不是加密值，则原样返回
    }
}

package org.goden.svdemo.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Integer id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 10, message = "用户名长度必须在2-10个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 16, message = "密码长度必须在6-16个字符之间")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,16}$",
            message = "密码必须包含至少一个字母和一个数字")
    private String password;

    private String nickname;

    private String email;

    private String userPic;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

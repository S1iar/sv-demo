package org.goden.svdemo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.goden.svdemo.anno.ValidationGroups;

import java.time.LocalDateTime;

@Data
// @JsonAutoDetect注解会将影响所有  public 的 getter 方法导致反序列化和序列化都进行了JsonIgnore
//@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class User {

    private Integer id;

    @NotBlank(groups = {ValidationGroups.Create.class,ValidationGroups.Login.class},
            message = "用户名不能为空")
    @Size(groups = ValidationGroups.Create.class,
            min = 3, max = 10, message = "用户名长度必须在5-16个字符之间")
    private String username;

    // @JsonProperty 字段序列化时会被忽略，即它不会出现在生成的JSON中。但在反序列化时，JSON中的相应数据会被正常赋值给该属性
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(groups = {ValidationGroups.Create.class, ValidationGroups.PasswordCheck.class,ValidationGroups.Login.class},
              message = "密码不能为空!")
    @Size(groups = {ValidationGroups.Create.class, ValidationGroups.PasswordCheck.class},
            min = 6, max = 16, message = "密码长度必须在6-16个字符之间!")
    @Pattern(groups = {ValidationGroups.Create.class, ValidationGroups.PasswordCheck.class},
            regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).{6,16}$",
            message = "密码必须包含至少一个字母和一个数字!")
    private String password;

    private String nickname;

    @Email(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class, ValidationGroups.EmailCheck.class},
           message = "邮箱格式不正确")
    private String email;

    private String userPic;

    private String phone;

    private boolean enabled;

    private boolean accountNonExpired;

    private boolean credentialsNonExpired;

    private boolean accountNonLocked;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

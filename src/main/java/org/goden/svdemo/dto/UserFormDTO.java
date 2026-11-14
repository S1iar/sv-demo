package org.goden.svdemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.goden.svdemo.anno.ValidationGroups;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户信息")
public class UserFormDTO {
    @Schema(description = "用户ID")
    private Long id;
    @Schema(description = "用户名")
    @NotBlank(groups = {ValidationGroups.Create.class,ValidationGroups.Login.class},
            message = "用户名不能为空")
    @Size(groups = ValidationGroups.Create.class,
            min = 3, max = 10, message = "用户名长度必须在3-10个字符之间")
    private String username;

    @Schema(description = "密码")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(groups = {ValidationGroups.Create.class, ValidationGroups.PasswordCheck.class,ValidationGroups.Login.class},
            message = "密码不能为空!")
    @Size(groups = {ValidationGroups.Create.class, ValidationGroups.PasswordCheck.class},
            min = 6, max = 16, message = "密码长度必须在6-16个字符之间!")
    @Pattern(groups = {ValidationGroups.Create.class, ValidationGroups.PasswordCheck.class},
            regexp = "^(?=.*[0-9])(?=.*[a-zA-Z]).*$",
            message = "密码必须包含至少一个字母和一个数字!")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    @NotBlank(groups = ValidationGroups.Create.class, message = "邮箱不能为空!")
    @Email(groups = {ValidationGroups.Create.class, ValidationGroups.Update.class, ValidationGroups.EmailCheck.class},
            message = "邮箱格式不正确")
    private String email;

    @Schema(description = "用户头像")
    private String userPic;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "地址")
    private String address;
}

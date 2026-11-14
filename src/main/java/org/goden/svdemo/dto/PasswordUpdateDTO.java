package org.goden.svdemo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改密码请求参数")
public class PasswordUpdateDTO {

    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @Schema(description = "新的密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    @Schema(description = "二次验证密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rePassword;
}

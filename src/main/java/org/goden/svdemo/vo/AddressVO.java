package org.goden.svdemo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户地址信息")
public class AddressVO {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "地址路径")
    private String addressPath;

    @Schema(description = "市区")
    private String city;

    @Schema(description = "城镇")
    private String town;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "街道")
    private String street;

    @Schema(description = "门牌号")
    private String houseNum;
}

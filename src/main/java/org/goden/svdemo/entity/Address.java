package org.goden.svdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author sliar
 * @since 2026-11-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("address")
@Schema(description="Address对象")
public class Address implements Serializable {

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
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

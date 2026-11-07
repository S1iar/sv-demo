package org.goden.svdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("permission")
@Data
public class Permission {
    private Long id;
    private String permCode;
    private String permName;
}
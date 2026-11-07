package org.goden.svdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("role")
@Data
public class Role {
    private Long id;
    private String roleName;
    private String roleDesc;
}

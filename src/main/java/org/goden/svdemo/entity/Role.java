package org.goden.svdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;

@TableName("role")
@Data
public class Role {
    @Id
    private Long id;
    private String roleName;
    private String roleDesc;
}

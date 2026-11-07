package org.goden.svdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;

@TableName("permission")
@Data
public class Permission {
    @Id
    private Long id;
    private String permCode;
    private String permName;
}
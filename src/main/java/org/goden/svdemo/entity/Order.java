package org.goden.svdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long userId;

    private Integer stock;

    private Integer version;  // 仅用于演示乐观锁，本例不使用

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

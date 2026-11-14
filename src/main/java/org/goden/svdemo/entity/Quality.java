package org.goden.svdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Quality {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer value1;
    private Integer value2;
    private Long InspectorId;
}

package org.goden.svdemo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE(1, "活动"),
    INACTIVE(0, "不活动"),
    PENDING(2, "待处理"),
    FROZEN(3, "冻结");

    @EnumValue
    private final int value;
    // 前端响应时,使用desc字段
    @JsonValue
    private final String desc;

    UserStatus(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}

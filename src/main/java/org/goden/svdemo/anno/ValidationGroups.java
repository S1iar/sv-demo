package org.goden.svdemo.anno;

// 创建验证分组
public interface ValidationGroups {
    interface Create {}  // 创建时的验证组
    interface Login {}  // 创建时的验证组
    interface Update {}  // 更新时的验证组
    interface EmailCheck {}  // 仅邮箱验证组
    interface PasswordCheck {}  // 仅密码验证组
}

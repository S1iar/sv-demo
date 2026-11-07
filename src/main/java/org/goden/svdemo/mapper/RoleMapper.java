package org.goden.svdemo.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.goden.svdemo.entity.User;

@Mapper
public interface RoleMapper {
    // role_id:2,默认使用普通用户权限
    void add(User user);
}

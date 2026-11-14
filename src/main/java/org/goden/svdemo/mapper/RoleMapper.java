package org.goden.svdemo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.goden.svdemo.entity.Role;
import org.goden.svdemo.entity.User;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    // role_id:2,默认使用普通用户权限
    void add(User user);
}

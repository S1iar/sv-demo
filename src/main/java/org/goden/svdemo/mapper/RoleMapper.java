package org.goden.svdemo.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.goden.svdemo.entity.User;

@Mapper
public interface RoleMapper {
    @Insert("INSERT INTO user_role(user_id, role_id) VALUES(#{id},2)")
    void add(User user);
}

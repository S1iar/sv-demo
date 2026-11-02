package org.goden.svdemo.mapper;

import org.apache.ibatis.annotations.*;
import org.goden.svdemo.entity.Permission;
import org.goden.svdemo.entity.Role;
import org.goden.svdemo.entity.User;
import java.util.List;

@Mapper
public interface UserMapper {
    User findUserByUserName(String username);

    User findUserByUserNameAndPassword(String username, String password);

    User findUserById(Long id);

    void add(User user);

    void updateById(User user);

    void updateAvatarById(User user);

    void updatePassword(User user);

    List<Role> findRolesByUserId(Long userId);

    List<Permission> findPermissionsByUserId(Long userId);
}

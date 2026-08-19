package org.goden.svdemo.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.goden.svdemo.entity.Permission;
import org.goden.svdemo.entity.Role;
import org.goden.svdemo.entity.User;
import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM user WHERE username =#{username}")
    User findUserByUserName(String username);

    @Select("SELECT * FROM user WHERE username =#{username} AND password =#{password}")
    User findUserByUserNameAndPassword(String username, String password);

    @Select("SELECT * FROM user WHERE id =#{id}")
    User findUserById(Integer id);

    @Insert("INSERT INTO user(username,password,email,create_time,update_time)" +
            " VALUES(#{username},#{password},#{email},now(),now())")
    void add(User user);

    @Update("UPDATE user SET nickname=#{nickname},email=#{email},user_pic=#{userPic},update_time=now() WHERE id=#{id}")
    void updateById(User user);

    @Update("UPDATE user SET user_pic=#{userPic},update_time=now() WHERE id=#{id}")
    void updateAvatarById(User user);

    @Update("UPDATE user SET password=#{password},update_time=now() WHERE id=#{id}")
    void updatePassword(User user);

    // 查询用户拥有的角色列表
    @Select("SELECT r.id, r.role_name AS roleName, r.role_desc AS roleDesc " +
            "FROM role r INNER JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Role> findRolesByUserId(Integer userId);

    // 查询用户拥有的所有权限（通过角色间接获得）
    @Select("SELECT DISTINCT p.id, p.perm_code AS permCode, p.perm_name AS permName " +
            "FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Permission> findPermissionsByUserId(Integer userId);
}

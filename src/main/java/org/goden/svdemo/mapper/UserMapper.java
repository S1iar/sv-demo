package org.goden.svdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.goden.svdemo.entity.Role;
import org.goden.svdemo.entity.User;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User>{
    User findUserByUserName(String username);

    User findUserByUserNameAndPassword(String username, String password);

    User findUserById(Long id);

    void add(User user);

//    void updateById(User user);
    int updateById(User user);

    void updateAvatarById(User user);

    void updatePassword(User user);

    List<Role> findRolesByUserId(Long userId);
}

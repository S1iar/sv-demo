package org.goden.svdemo.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.goden.svdemo.pojo.User;

@Mapper
public interface UserMapper {
    @Select("select * from user where username =#{username}")
    User findUserByUserName(String username);

    @Insert("insert into user(username,password,create_time,update_time)" +
            " values(#{username},#{password},now(),now())")
    void add(User user);

    @Update("update user set nickname=#{nickname},email=#{email},user_pic=#{userPic},update_time=#{updateTime} where id=#{id}")
    void update(User user);
}

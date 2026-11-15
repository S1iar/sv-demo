package org.goden.svdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.goden.svdemo.dto.PasswordUpdateDTO;
import org.goden.svdemo.entity.User;
import org.goden.svdemo.vo.UserVO;
import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {
    User findUserByUserName(String username);

    User findUserById(Long id);

    User findUserByUserNameAndPassword(String username, String password);

    void register(User user);

    Map<String, String> login(User user);

    void update(User user);

    void updateAvatar(String avatarUrl);

    void updatePassword(PasswordUpdateDTO passwordUpdateDTO);

    UserVO findUserAndAddressByUserId(Long userId);

    List<UserVO> findUserAndAddressByUserIds(List<Long> ids);
}

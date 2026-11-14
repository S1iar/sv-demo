package org.goden.svdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.goden.svdemo.entity.User;
import org.goden.svdemo.mapper.UserMapper;
import org.goden.svdemo.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class IUserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
}

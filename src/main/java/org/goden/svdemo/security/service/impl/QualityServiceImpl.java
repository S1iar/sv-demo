package org.goden.svdemo.security.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.goden.svdemo.entity.Quality;
import org.goden.svdemo.entity.User;
import org.goden.svdemo.exception.BusinessException;
import org.goden.svdemo.mapper.QualityMapper;
import org.goden.svdemo.security.service.QualityService;
import org.goden.svdemo.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QualityServiceImpl extends ServiceImpl<QualityMapper, Quality> implements QualityService {

    private final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductValue(Quality quality) {
        //1.查询quality
        Quality q = this.getById(quality.getId());
        //2.校验InspectorId状态
        User inspector = userService.findUserById(q.getInspectorId());
        if (!inspector.isEnabled()) {
            throw new BusinessException("检查员已弃用!");
        }
        //3.扣减value
        this.updateById(quality);
    }
}

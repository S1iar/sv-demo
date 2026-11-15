package org.goden.svdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.goden.svdemo.entity.Quality;
import org.goden.svdemo.entity.User;
import org.goden.svdemo.exception.BusinessException;
import org.goden.svdemo.mapper.QualityMapper;
import org.goden.svdemo.service.QualityService;
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
        if(q == null){
            throw new BusinessException("质量记录不存在!");
        }
        //2.校验InspectorId状态
        User inspector = userService.findUserById(q.getInspectorId());
        if (inspector == null || !inspector.isEnabled()) {
            throw new BusinessException("检查员状态异常!");
        }
        //3.尝试扣减value
        Integer value1 = q.getValue1();
        Integer value2 = q.getValue2();
        lambdaUpdate()
                .set(value1 - quality.getValue1() >= 0, Quality::getValue1, value1 - quality.getValue1())
                .set(value2 - quality.getValue2() >= 0,Quality::getValue2, value2 - quality.getValue2())
                .eq(Quality::getId, quality.getId())
                .update();
        baseMapper.deductValue(quality);
    }
}

package org.goden.svdemo.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.goden.svdemo.entity.Quality;

public interface QualityService extends IService<Quality> {
    void deductValue(Quality quality);
}

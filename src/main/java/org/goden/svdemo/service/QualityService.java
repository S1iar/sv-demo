package org.goden.svdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.goden.svdemo.entity.Quality;

public interface QualityService extends IService<Quality> {
    void deductValue(Quality quality);
}

package org.goden.svdemo.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.goden.svdemo.entity.Quality;

@Mapper
public interface QualityMapper extends BaseMapper<Quality> {

//    void testQuality(@Param(Constants.WRAPPER) Wrapper<Quality> wrapper);
//    void testQuality(@Param("ew") Wrapper<Quality> wrapper);

    void deductValue(@Param("quality") Quality quality);


}

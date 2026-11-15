package org.goden.svdemo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.goden.svdemo.entity.Address;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author sliar
 * @since 2026-11-15
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {

}

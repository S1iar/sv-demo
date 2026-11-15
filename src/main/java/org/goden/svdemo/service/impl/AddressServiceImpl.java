package org.goden.svdemo.service.impl;

import org.goden.svdemo.entity.Address;
import org.goden.svdemo.mapper.AddressMapper;
import org.goden.svdemo.service.IAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author sliar
 * @since 2026-11-15
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {

}

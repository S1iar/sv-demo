package org.goden.svdemo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.goden.svdemo.entity.Permission;
import java.util.List;

@Mapper
public interface PermissionMapper {
    List<Permission> findPermissionsByUserId(Long userId);
}

package org.goden.svdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.goden.svdemo.entity.Order;

public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 自定义方法：根据 productId 加悲观锁查询
     */
    @Select("SELECT * FROM t_order WHERE product_id = #{productId} FOR UPDATE")
    Order selectByProductIdWithLock(@Param("productId") Long productId);
}

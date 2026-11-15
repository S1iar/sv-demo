package org.goden.svdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.goden.svdemo.entity.Order;
import org.goden.svdemo.mapper.OrderMapper;
import org.goden.svdemo.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    /**
     * 方案A：使用自定义 Mapper 方法（推荐）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStockByCustomMethod(Long productId, Integer count) {
        // 1. 加悲观锁查询
        Order order = baseMapper.selectByProductIdWithLock(productId);
        if (order == null || order.getStock() < count) {
            throw new RuntimeException("库存不足");
        }

        // 2. 执行更新（使用 MP 的 lambdaUpdate + setSql 实现原子自减）
        boolean updated = update(new LambdaUpdateWrapper<Order>()
                .eq(Order::getProductId, productId)
                .setSql("stock = stock - " + count));
        return updated;
    }

    /**
     * 方案B：完全使用 Lambda + last("FOR UPDATE")（不推荐，但可演示）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStockByLambdaLast(Long productId, Integer count) {
        // 1. 加悲观锁查询
        Order order = baseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getProductId, productId)
                        .last("FOR UPDATE")
        );
        if (order == null || order.getStock() < count) {
            throw new RuntimeException("库存不足");
        }

        // setSql("stock = stock - " + count) 用了字符串拼接，如果 count 来自外部输入（如 HTTP 请求参数）,存在 SQL 注入风险.改为占位符形式
        // 保留两位小数  {0}代表setSql 方法中逗号后面的第一个参数,{1}则代表第二个参数
        // .setSql("price = price * {0,number,#.##}", discountRate)
        // 日期格式化
        // .setSql("create_time = {0,date,yyyy-MM-dd HH:mm:ss}", now)
        return lambdaUpdate()
                .eq(Order::getProductId, productId)
                .setSql("stock = stock - {0}", count)
                .update();
    }
}

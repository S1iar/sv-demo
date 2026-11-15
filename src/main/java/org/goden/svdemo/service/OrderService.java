package org.goden.svdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.goden.svdemo.entity.Order;

public interface OrderService extends IService<Order> {
    boolean deductStockByCustomMethod(Long productId, Integer count);
    boolean deductStockByLambdaLast(Long productId, Integer count);
}

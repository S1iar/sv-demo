package org.goden.svdemo.controller;

import lombok.RequiredArgsConstructor;
import org.goden.svdemo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 使用自定义 Mapper 方法扣减库存（推荐）
     */
    @PostMapping("/deduct/custom/{productId}")
    public String deductByCustom(@PathVariable Long productId,
                                 @RequestParam(defaultValue = "1") Integer count) {
        try {
            orderService.deductStockByCustomMethod(productId, count);
            return "扣减成功";
        } catch (Exception e) {
            return "失败：" + e.getMessage();
        }
    }

    /**
     * 使用 Lambda + last 扣减库存（演示用）
     */
    @PostMapping("/deduct/lambda/{productId}")
    public String deductByLambda(@PathVariable Long productId,
                                 @RequestParam(defaultValue = "1") Integer count) {
        try {
            orderService.deductStockByLambdaLast(productId, count);
            return "扣减成功";
        } catch (Exception e) {
            return "失败：" + e.getMessage();
        }
    }
}

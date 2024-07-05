package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    /**
     * 创建订单
     *
     * @param userId
     * @param voucherId
     * @param dbVoucher
     * @return
     */
    long createOrder(Long userId, Long voucherId, SeckillVoucher dbVoucher);
}

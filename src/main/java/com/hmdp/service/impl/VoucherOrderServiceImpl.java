package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.BizException;
import com.hmdp.utils.RedisUtils;
import com.hmdp.utils.SessionHolder;
import com.hmdp.utils.SimpleDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 秒杀
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
//        return safeInSingleNode(voucherId);//单机情况下线程安全
        return safeInMutexNode(voucherId);//集群情况下线程安全
    }

    /**
     * 分布式锁实现的集群情况下线程安全
     *
     * @param voucherId
     * @return
     */
    private Result safeInMutexNode(Long voucherId) {
        // 1.查询优惠价信息
        SeckillVoucher dbVoucher = iSeckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (dbVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }

        // 3.判断秒杀是否结束
        if (dbVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }

        // 4.1判断库存是否充足
        if (dbVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long userId = SessionHolder.getUser().getId();

        if (userId == null) {
            throw new BizException("请先登录");
        }
        // 4.2判断用户是否购买过该优惠券
        Integer countUserOrder = voucherOrderService.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (countUserOrder > 0) {
            throw new BizException("您已购买过该优惠券");
        }

        SimpleDistributedLock orderLock = new SimpleDistributedLock("order", redisUtils.strRedisTemplate);

        try {
            orderLock.tryLock(1000);
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            long orderId = proxy.createOrder(userId, voucherId, dbVoucher);
            return Result.ok(orderId);
        } finally {
            orderLock.unlock();
        }
    }

    /**
     * 单机环境下线程安全的秒杀
     *
     * @param voucherId
     * @return
     */
    private Result safeInSingleNode(Long voucherId) {
        // 1.查询优惠价信息
        SeckillVoucher dbVoucher = iSeckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始
        if (dbVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }

        // 3.判断秒杀是否结束
        if (dbVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }

        // 4.1判断库存是否充足
        if (dbVoucher.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long userId = SessionHolder.getUser().getId();

        if (userId == null) {
            throw new BizException("请先登录");
        }
        // 4.2判断用户是否购买过该优惠券
        Integer countUserOrder = voucherOrderService.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (countUserOrder > 0) {
            throw new BizException("您已购买过该优惠券");
        }
        synchronized (userId.toString().intern()) {
            VoucherOrderServiceImpl proxy = (VoucherOrderServiceImpl) AopContext.currentProxy();
            long orderId = proxy.createOrder(userId, voucherId, dbVoucher);
            return Result.ok(orderId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public long createOrder(Long userId, Long voucherId, SeckillVoucher dbVoucher) {


        // 5.减库存
        boolean done = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                //乐观锁解决超卖问题
                .gt("stock", 0)
                .update();
        if (!done) {
            throw new BizException("减库存失败");
        }


        // 6.生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisUtils.nextDistributedID("voucher_order");
        voucherOrder.setId(orderId);

        voucherOrder.setUserId(userId);

        voucherOrder.setVoucherId(dbVoucher.getVoucherId());
        log.info("voucherOrder: {}", voucherOrder);
        save(voucherOrder);
        return orderId;
    }
}

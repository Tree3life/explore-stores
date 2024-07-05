package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.BizException;
import com.hmdp.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private RedisUtils redisUtils;


    @Override
    public Shop queryById(Long id) {
//        return queryByIdWithNothing(id);
//        return queryByIdCachePenetration(id);
        log.info("queryById查询商铺信息:{}", id);
        return queryByIdCacheBreakdownWithDistributedLock(id);
//        return queryByIdCacheBreakdownWithLogicExpire(id);
    }

    /**
     * 缓存穿透
     *
     * @param id
     * @return
     */
    public Shop queryByIdCachePenetration(Long id) {
        // 1.从redis中查询商铺信息
//        String shopJson = redisUtils.getCacheObject(RedisUtils.Constance.SHOP_KEY + id);
        String shopJson = redisUtils.getCacheObject(RedisUtils.Constant.SHOP_KEY + id);
        System.out.println("id:" + id + ",redis中商铺信息为：" + shopJson);
        // 1.1 redis中保存了有效的商铺信息，直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 1.2 redis中保存的商铺信息为空字符串""（即，不为空，但信息无效）,无效->直接返回
        if (shopJson != null) {
            throw new BizException("商铺不存在！");
        }

        //region处理缓存穿透
        // 2.没有商铺信息，从mysql中查询商铺信息
        // 2.1 查询mysql中的商铺信息
        Shop shop = this.getById(id);
        if (null == shop) {
            //写入空串 防止缓存穿透
            redisUtils.setCacheObject(RedisUtils.Constant.SHOP_KEY + id, "", RedisUtils.Constant.CacheNullTTL, TimeUnit.MINUTES);
            throw new BizException("查询不到商铺信息");
        }
        //endregion处理缓存穿透


        // 2.2 将查询到的商铺信息放入redis中
        redisUtils.setCacheObject(RedisUtils.Constant.SHOP_KEY + id, JSONUtil.toJsonStr(shop), 30, TimeUnit.MINUTES);
        // 返回查询到的商铺信息
        return shop;
    }

    /**
     * 缓存击穿  分布式锁实现
     *
     * @param id
     * @return
     */
    public Shop queryByIdCacheBreakdownWithDistributedLock(Long id) {
        return redisUtils.queryWithMutex(
                RedisUtils.Constant.SHOP_KEY + id,
                id,
                Shop.class,
                (params) -> {
                    //查询mysql，缓存重建的业务逻辑
                    log.info("缓存重建的业务逻辑:{}", id);
                    return this.getById(params);
                },
                10l,
                TimeUnit.SECONDS);
    }

    /**
     * 缓存击穿  逻辑过期实现
     *
     * @param id
     * @return
     */
    public Shop queryByIdCacheBreakdownWithLogicExpire(Long id) {
        return redisUtils.queryWithLogicalExpire(
                RedisUtils.Constant.SHOP_KEY + id,
                id,
                Shop.class,
                (params) -> {
                    //查询mysql，缓存重建的业务逻辑
                    return this.getById(params);
                },
                10l,
                TimeUnit.SECONDS);
    }

    /**
     * 未考虑缓存穿透/击穿的实现
     *
     * @param id
     * @return
     */
    public Shop queryByIdWithNothing(Long id) {
        // 1.从redis中查询商铺信息
//        String shopJson = redisUtils.getCacheObject(RedisUtils.Constance.SHOP_KEY + id);
        String shopJson = redisUtils.getCacheObject(RedisUtils.Constant.SHOP_KEY + id);
        log.info("id:" + id + ",redis中商铺信息为：" + shopJson);
        // 1.1 redis中保存了有效的商铺信息，直接返回
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 2.1 查询mysql中的商铺信息
        Shop shop = this.getById(id);
        // 2.2 将查询到的商铺信息放入redis中
        redisUtils.setCacheObject(RedisUtils.Constant.SHOP_KEY + id, JSONUtil.toJsonStr(shop), 30, TimeUnit.MINUTES);

        // 返回查询到的商铺信息
        return shop;
    }

    @Override
    public void updateShopById(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            throw new BizException("店铺id不能为空");
        }
        // 1.先更新数据库中对应的信息
        this.updateById(shop);
        // 2.删除缓存中对应的信息
        redisUtils.deleteObject(RedisUtils.Constant.SHOP_KEY + id);
    }
}

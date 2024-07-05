package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private RedisUtils redisUtils;

    /**
     * List<ShopType> typeList = typeService
     * //                .query().orderByAsc("sort").list();
     *
     * @return
     */
    @Override
    public List<ShopType> queryShopType() {
        // 1.查redis
        List<ShopType> typeList = redisUtils.getCacheObject(RedisUtils.Constant.TYPELIST_KEY);
        // 1.2有效 直接返回
        if (CollectionUtil.isNotEmpty(typeList)) {
            return typeList;
        }

        // 2.无效 查mysql
        typeList = query().orderByAsc("sort").list();
        // 2.1 是否有效 有效存入redis
        if (typeList != null) {
            redisUtils.setCacheObject(RedisUtils.Constant.TYPELIST_KEY, typeList);
        }
        // 2.2 返回结果
        return typeList;
    }
}

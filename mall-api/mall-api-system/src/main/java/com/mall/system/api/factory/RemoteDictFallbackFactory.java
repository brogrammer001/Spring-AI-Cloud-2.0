package com.mall.system.api.factory;

import com.mall.common.core.domain.R;
import com.mall.system.api.RemoteDictService;
import com.mall.system.api.domain.SysDictData;
import com.mall.system.api.domain.SysDictType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典服务降级处理
 * 
 * @author mall
 */
@Component
public class RemoteDictFallbackFactory implements FallbackFactory<RemoteDictService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteDictFallbackFactory.class);

    @Override
    public RemoteDictService create(Throwable throwable)
    {
        log.error("字典服务调用失败:{}", throwable.getMessage());
        return new RemoteDictService()
        {
            @Override
            public R<List<SysDictType>> getDictTypeList(SysDictType dictType)
            {
                return R.fail("查询字典类型失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addDictType(SysDictType dictType)
            {
                return R.fail("新增字典类型失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> updateDictType(SysDictType dictType)
            {
                return R.fail("修改字典类型失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deleteDictType(Long dictId)
            {
                return R.fail("删除字典类型失败:" + throwable.getMessage());
            }

            @Override
            public R<List<SysDictData>> getDictDataList(SysDictData dictData)
            {
                return R.fail("查询字典数据失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> addDictData(SysDictData dictData)
            {
                return R.fail("新增字典数据失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> updateDictData(SysDictData dictData)
            {
                return R.fail("修改字典数据失败:" + throwable.getMessage());
            }

            @Override
            public R<Boolean> deleteDictData(Long dictCode)
            {
                return R.fail("删除字典数据失败:" + throwable.getMessage());
            }
        };
    }
}
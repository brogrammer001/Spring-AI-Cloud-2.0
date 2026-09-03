package com.mall.system.api;

import java.util.List;
import com.mall.common.core.constant.ServiceNameConstants;
import com.mall.common.core.domain.R;
import com.mall.system.api.domain.SysDictData;
import com.mall.system.api.domain.SysDictType;
import com.mall.system.api.factory.RemoteDictFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 字典服务
 * 
 * @author mall
 */
@FeignClient(contextId = "remoteDictService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteDictFallbackFactory.class)
public interface RemoteDictService {

    @PostMapping("/api/dict/type/list")
    public R<List<SysDictType>> getDictTypeList(@RequestBody SysDictType dictType);

    @PostMapping("/api/dict/type/add")
    public R<Boolean> addDictType(@RequestBody SysDictType dictType);

    @PutMapping("/api/dict/type/update")
    public R<Boolean> updateDictType(@RequestBody SysDictType dictType);

    @DeleteMapping("/api/dict/type/{dictId}")
    public R<Boolean> deleteDictType(@PathVariable("dictId") Long dictId);

    @PostMapping("/api/dict/data/list")
    public R<List<SysDictData>> getDictDataList(@RequestBody SysDictData dictData);

    @PostMapping("/api/dict/data/add")
    public R<Boolean> addDictData(@RequestBody SysDictData dictData);

    @PutMapping("/api/dict/data/update")
    public R<Boolean> updateDictData(@RequestBody SysDictData dictData);

    @DeleteMapping("/api/dict/data/{dictCode}")
    public R<Boolean> deleteDictData(@PathVariable("dictCode") Long dictCode);

}
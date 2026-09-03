package com.mall.system.api;

import com.mall.common.core.domain.R;
import com.mall.common.core.web.controller.BaseController;
import com.mall.common.security.annotation.InnerAuth;
import com.mall.system.api.domain.SysDictData;
import com.mall.system.api.domain.SysDictType;
import com.mall.system.service.ISysDictDataService;
import com.mall.system.service.ISysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dict")
public class SysDictApi extends BaseController {

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private ISysDictDataService dictDataService;

    @PostMapping("/type/list")
    @InnerAuth
    public R<List<SysDictType>> getDictTypeList(@RequestBody SysDictType dictType) {
        List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
        return R.ok(list);
    }

    @PostMapping("/type/add")
    @InnerAuth
    public R<Boolean> addDictType(@RequestBody SysDictType dictType) {
        return R.ok(dictTypeService.insertDictType(dictType) > 0);
    }

    @PutMapping("/type/update")
    @InnerAuth
    public R<Boolean> updateDictType(@RequestBody SysDictType dictType) {
        return R.ok(dictTypeService.updateDictType(dictType) > 0);
    }

    @DeleteMapping("/type/{dictId}")
    @InnerAuth
    public R<Boolean> deleteDictType(@PathVariable("dictId") Long dictId) {
        dictTypeService.deleteDictTypeByIds(new Long[]{dictId});
        return R.ok(true);
    }

    @PostMapping("/data/list")
    @InnerAuth
    public R<List<SysDictData>> getDictDataList(@RequestBody SysDictData dictData) {
        List<SysDictData> list = dictDataService.selectDictDataList(dictData);
        return R.ok(list);
    }

    @PostMapping("/data/add")
    @InnerAuth
    public R<Boolean> addDictData(@RequestBody SysDictData dictData) {
        return R.ok(dictDataService.insertDictData(dictData) > 0);
    }

    @PutMapping("/data/update")
    @InnerAuth
    public R<Boolean> updateDictData(@RequestBody SysDictData dictData) {
        return R.ok(dictDataService.updateDictData(dictData) > 0);
    }

    @DeleteMapping("/data/{dictCode}")
    @InnerAuth
    public R<Boolean> deleteDictData(@PathVariable("dictCode") Long dictCode) {
        dictDataService.deleteDictDataByIds(new Long[]{dictCode});
        return R.ok(true);
    }

}
package com.dqv5.autogennum.web;


import com.dqv5.autogennum.common.response.Return;
import com.dqv5.autogennum.common.response.ReturnEntity;
import com.dqv5.autogennum.entity.AutoNumberRecord;
import com.dqv5.autogennum.entity.AutoNumberRule;
import com.dqv5.autogennum.service.AutoNumberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


/**
 * 自动编号规则
 */
@Slf4j
@RestController
@RequestMapping("/autonumber")
public class AutoNumberController {

    @Resource
    private AutoNumberService autoNumberService;

    @GetMapping(value = "/queryAllList")
    public ResponseEntity<ReturnEntity<List<AutoNumberRule>>> queryAllList() {
        List<AutoNumberRule> list = autoNumberService.queryAllList();
        return Return.build(true, "查询成功", list);
    }

    @PostMapping(value = "/insert")
    public ResponseEntity<ReturnEntity<Object>> insert(@RequestBody AutoNumberRule autoNumberRule) {
        autoNumberService.insert(autoNumberRule);
        return Return.build(true, "新增成功");
    }


    @PostMapping(value = "/update")
    public ResponseEntity<ReturnEntity<Object>> update(@RequestBody AutoNumberRule autoNumberRule) {
        autoNumberService.update(autoNumberRule);
        return Return.build(true, "更新成功");
    }

    @PostMapping(value = "/delete/{ruleId}")
    public ResponseEntity<ReturnEntity<Object>> delete(@PathVariable int ruleId) {
        autoNumberService.delete(ruleId);
        return Return.build(true, "删除成功");
    }


    @PostMapping(value = "/generate/{ruleId}")
    public ResponseEntity<ReturnEntity<Object>> generate(@PathVariable int ruleId) {
        String number = autoNumberService.generateNextNumber(ruleId);
        return Return.build(true, "生成编号成功", number);
    }

    @PostMapping(value = "/safeGenerate/{ruleId}")
    public ResponseEntity<ReturnEntity<Object>> safeGenerate(@PathVariable int ruleId) {
        String number = autoNumberService.safeGenerateNextNumber(ruleId);
        return Return.build(true, "生成编号成功", number);
    }

    @GetMapping(value = "/recordList")
    public ResponseEntity<ReturnEntity<List<AutoNumberRecord>>> recordList() {
        List<AutoNumberRecord> list = autoNumberService.queryRecordList();
        return Return.build(true, "查询成功", list);
    }


}

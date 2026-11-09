package com.smarthome.environment.controller;

import com.smarthome.common.result.Result;
import com.smarthome.environment.dto.FamilyRequest;
import com.smarthome.environment.service.FamilyService;
import com.smarthome.environment.vo.FamilyVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S1 家庭信息管理接口。
 */
@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @PostMapping
    public Result<FamilyVO> create(@Valid @RequestBody FamilyRequest request) {
        return Result.success("创建成功", familyService.create(request));
    }

    @GetMapping
    public Result<List<FamilyVO>> list() {
        return Result.success(familyService.list());
    }

    @GetMapping("/{id}")
    public Result<FamilyVO> getById(@PathVariable("id") Long id) {
        return Result.success(familyService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<FamilyVO> update(@PathVariable("id") Long id, @Valid @RequestBody FamilyRequest request) {
        return Result.success("修改成功", familyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        familyService.delete(id);
        return Result.success();
    }
}

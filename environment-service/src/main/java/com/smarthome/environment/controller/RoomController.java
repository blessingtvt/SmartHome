package com.smarthome.environment.controller;

import com.smarthome.common.result.Result;
import com.smarthome.environment.dto.RoomRequest;
import com.smarthome.environment.service.RoomService;
import com.smarthome.environment.vo.RoomVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S1 房间信息管理接口。
 */
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public Result<RoomVO> create(@Valid @RequestBody RoomRequest request) {
        return Result.success("创建成功", roomService.create(request));
    }

    @GetMapping
    public Result<List<RoomVO>> list(@RequestParam(value = "familyId", required = false) Long familyId) {
        return Result.success(roomService.list(familyId));
    }

    @GetMapping("/{id}")
    public Result<RoomVO> getById(@PathVariable("id") Long id) {
        return Result.success(roomService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<RoomVO> update(@PathVariable("id") Long id, @Valid @RequestBody RoomRequest request) {
        return Result.success("修改成功", roomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        roomService.delete(id);
        return Result.success();
    }
}

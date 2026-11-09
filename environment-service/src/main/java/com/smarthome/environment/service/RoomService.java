package com.smarthome.environment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smarthome.common.exception.BusinessException;
import com.smarthome.common.result.ResultCode;
import com.smarthome.common.utils.TimeUtils;
import com.smarthome.environment.dto.RoomRequest;
import com.smarthome.environment.entity.Room;
import com.smarthome.environment.mapper.RoomMapper;
import com.smarthome.environment.vo.RoomVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 房间信息业务逻辑（S1）。
 */
@Service
public class RoomService {

    private final RoomMapper roomMapper;

    public RoomService(RoomMapper roomMapper) {
        this.roomMapper = roomMapper;
    }

    public RoomVO create(RoomRequest request) {
        Room entity = new Room();
        entity.setFamilyId(request.getFamilyId());
        entity.setRoomName(request.getRoomName());
        entity.setCreateTime(TimeUtils.now());
        roomMapper.insert(entity);
        return toVO(entity);
    }

    /** 按家庭查询房间列表；familyId 为空时返回全部 */
    public List<RoomVO> list(Long familyId) {
        QueryWrapper<Room> wrapper = new QueryWrapper<>();
        if (familyId != null) {
            wrapper.eq("family_id", familyId);
        }
        wrapper.orderByAsc("id");
        return roomMapper.selectList(wrapper).stream()
                .map(RoomService::toVO)
                .toList();
    }

    public RoomVO getById(Long id) {
        return toVO(requireEntity(id));
    }

    public RoomVO update(Long id, RoomRequest request) {
        Room entity = requireEntity(id);
        entity.setFamilyId(request.getFamilyId());
        entity.setRoomName(request.getRoomName());
        roomMapper.updateById(entity);
        return toVO(entity);
    }

    public void delete(Long id) {
        requireEntity(id);
        roomMapper.deleteById(id);
    }

    private Room requireEntity(Long id) {
        Room entity = roomMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "房间不存在: id=" + id);
        }
        return entity;
    }

    static RoomVO toVO(Room entity) {
        RoomVO vo = new RoomVO();
        vo.setId(entity.getId());
        vo.setFamilyId(entity.getFamilyId());
        vo.setRoomName(entity.getRoomName());
        vo.setCreateTime(TimeUtils.toEpochMillis(entity.getCreateTime()));
        return vo;
    }
}

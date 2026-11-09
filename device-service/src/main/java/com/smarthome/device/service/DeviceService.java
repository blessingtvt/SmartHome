package com.smarthome.device.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smarthome.common.constant.CommonConstant;
import com.smarthome.common.exception.BusinessException;
import com.smarthome.common.result.ResultCode;
import com.smarthome.common.utils.TimeUtils;
import com.smarthome.device.dto.DeviceRequest;
import com.smarthome.device.entity.Device;
import com.smarthome.device.entity.DeviceCommand;
import com.smarthome.device.mapper.DeviceCommandMapper;
import com.smarthome.device.mapper.DeviceMapper;
import com.smarthome.device.vo.DeviceVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备信息业务逻辑（S3）。
 * 只操作 S3 自己的 device 表；device_command 仅用于删除前检查未完成指令。
 */
@Service
public class DeviceService {

    private static final String ONLINE_STATUS_OFFLINE = "OFFLINE";
    private static final String ONLINE_STATUS_ONLINE = "ONLINE";
    private static final String DEVICE_STATUS_OFF = "OFF";

    private final DeviceMapper deviceMapper;
    private final DeviceCommandMapper deviceCommandMapper;

    public DeviceService(DeviceMapper deviceMapper, DeviceCommandMapper deviceCommandMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceCommandMapper = deviceCommandMapper;
    }

    /** 设备列表（按 id 升序） */
    public List<DeviceVO> list() {
        return deviceMapper.selectList(new QueryWrapper<Device>().orderByAsc("id")).stream()
                .map(DeviceService::toVO)
                .toList();
    }

    /** 新增设备：必要字段由 Controller @Valid 校验，此处校验 device_code 唯一性 */
    public DeviceVO create(DeviceRequest request) {
        requireDeviceCodeAvailable(request.getDeviceCode());
        Device entity = new Device();
        entity.setDeviceCode(request.getDeviceCode());
        entity.setDeviceName(request.getDeviceName());
        entity.setDeviceType(request.getDeviceType());
        entity.setRoomId(request.getRoomId());
        entity.setOnlineStatus(ONLINE_STATUS_OFFLINE);
        entity.setDeviceStatus(DEVICE_STATUS_OFF);
        entity.setCreateTime(TimeUtils.now());
        deviceMapper.insert(entity);
        return toVO(entity);
    }

    public DeviceVO getById(Long id) {
        return toVO(requireEntity(id));
    }

    /** 心跳上报：更新最后心跳时间并置为在线（ESP32 网络连接状态判定） */
    public DeviceVO heartbeat(String deviceCode) {
        Device entity = deviceMapper.selectOne(new QueryWrapper<Device>().eq("device_code", deviceCode));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "设备不存在: " + deviceCode);
        }
        entity.setOnlineStatus(ONLINE_STATUS_ONLINE);
        entity.setLastHeartbeatTime(TimeUtils.now());
        deviceMapper.updateById(entity);
        return toVO(entity);
    }

    /** 修改设备：不存在则 404；修改 deviceCode 时校验新编码不与其他设备冲突 */
    public DeviceVO update(Long id, DeviceRequest request) {
        Device entity = requireEntity(id);
        if (!entity.getDeviceCode().equals(request.getDeviceCode())) {
            requireDeviceCodeAvailable(request.getDeviceCode());
        }
        entity.setDeviceCode(request.getDeviceCode());
        entity.setDeviceName(request.getDeviceName());
        entity.setDeviceType(request.getDeviceType());
        entity.setRoomId(request.getRoomId());
        deviceMapper.updateById(entity);
        return toVO(entity);
    }

    /** 删除设备：存在未完成（PENDING）指令时禁止删除 */
    public void delete(Long id) {
        Device entity = requireEntity(id);
        Long pendingCount = deviceCommandMapper.selectCount(new QueryWrapper<DeviceCommand>()
                .eq("device_code", entity.getDeviceCode())
                .eq("status", CommonConstant.COMMAND_PENDING));
        if (pendingCount != null && pendingCount > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "该设备存在未完成指令，无法删除: " + entity.getDeviceCode());
        }
        deviceMapper.deleteById(id);
    }

    private Device requireEntity(Long id) {
        Device entity = deviceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "设备不存在: id=" + id);
        }
        return entity;
    }

    private void requireDeviceCodeAvailable(String deviceCode) {
        Long count = deviceMapper.selectCount(new QueryWrapper<Device>().eq("device_code", deviceCode));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "设备编码已存在: " + deviceCode);
        }
    }

    /** 实体 -> 对外 VO（时间转 epoch 毫秒，lastHeartbeatTime 可空） */
    static DeviceVO toVO(Device entity) {
        DeviceVO vo = new DeviceVO();
        vo.setId(entity.getId());
        vo.setDeviceCode(entity.getDeviceCode());
        vo.setDeviceName(entity.getDeviceName());
        vo.setDeviceType(entity.getDeviceType());
        vo.setRoomId(entity.getRoomId());
        vo.setOnlineStatus(entity.getOnlineStatus());
        vo.setDeviceStatus(entity.getDeviceStatus());
        vo.setCreateTime(TimeUtils.toEpochMillis(entity.getCreateTime()));
        vo.setLastHeartbeatTime(entity.getLastHeartbeatTime() == null
                ? null : TimeUtils.toEpochMillis(entity.getLastHeartbeatTime()));
        return vo;
    }
}

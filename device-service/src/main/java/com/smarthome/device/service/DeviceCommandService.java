package com.smarthome.device.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.smarthome.common.constant.CommonConstant;
import com.smarthome.common.exception.BusinessException;
import com.smarthome.common.result.ResultCode;
import com.smarthome.common.utils.TimeUtils;
import com.smarthome.device.dto.DeviceCommandCreateRequest;
import com.smarthome.device.dto.DeviceCommandResultRequest;
import com.smarthome.device.dto.DeviceRemoteCommandRequest;
import com.smarthome.device.entity.Device;
import com.smarthome.device.entity.DeviceCommand;
import com.smarthome.device.mapper.DeviceCommandMapper;
import com.smarthome.device.mapper.DeviceMapper;
import com.smarthome.device.vo.DeviceCommandVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 设备控制指令业务逻辑（S3-04）。
 *
 * <p>状态流转仅允许 PENDING -> SUCCESS / PENDING -> FAILED；
 * 结果上报采用条件更新（WHERE status='PENDING'）保证并发安全。</p>
 */
@Service
public class DeviceCommandService {

    /** INT02 内部指令来源（S2 规则服务），common 无此常量，本地定义 */
    private static final String SOURCE_AUTO_RULE = "AUTO_RULE";

    private final DeviceMapper deviceMapper;
    private final DeviceCommandMapper deviceCommandMapper;

    public DeviceCommandService(DeviceMapper deviceMapper, DeviceCommandMapper deviceCommandMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceCommandMapper = deviceCommandMapper;
    }

    /** API02：查询设备待执行指令（PENDING，按 create_time 升序） */
    public List<DeviceCommandVO> listPending(String deviceCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "deviceCode 不能为空");
        }
        requireDeviceExists(deviceCode);
        List<DeviceCommand> list = deviceCommandMapper.selectList(new QueryWrapper<DeviceCommand>()
                .eq("device_code", deviceCode)
                .eq("status", CommonConstant.COMMAND_PENDING)
                .orderByAsc("create_time"));
        return list.stream().map(DeviceCommandService::toVO).toList();
    }

    /** API08：家属远程控制设备（source 固定 REMOTE） */
    public DeviceCommandVO createRemoteCommand(Long deviceId, DeviceRemoteCommandRequest request) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "设备不存在: id=" + deviceId);
        }
        requireValidAction(request.getAction());
        return createCommand(device.getDeviceCode(), request.getAction(), CommonConstant.SOURCE_REMOTE);
    }

    /** INT02：内部创建指令（source 仅允许 AUTO_RULE） */
    public DeviceCommandVO createInternalCommand(DeviceCommandCreateRequest request) {
        requireDeviceExists(request.getDeviceCode());
        requireValidAction(request.getAction());
        if (!SOURCE_AUTO_RULE.equals(request.getSource())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "内部接口来源仅支持 AUTO_RULE");
        }
        return createCommand(request.getDeviceCode(), request.getAction(), SOURCE_AUTO_RULE);
    }

    /** API03：上报执行结果（并发安全：仅 PENDING 可更新为终态） */
    public DeviceCommandVO reportResult(Long commandId, DeviceCommandResultRequest request) {
        String status = request.getStatus();
        if (!CommonConstant.COMMAND_SUCCESS.equals(status) && !CommonConstant.COMMAND_FAILED.equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "执行结果仅支持 SUCCESS/FAILED");
        }
        DeviceCommand command = deviceCommandMapper.selectById(commandId);
        if (command == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "指令不存在: commandId=" + commandId);
        }
        if (!command.getDeviceCode().equals(request.getDeviceCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "设备编码与指令不匹配");
        }

        if (CommonConstant.COMMAND_PENDING.equals(command.getStatus())) {
            int updated = deviceCommandMapper.update(null, new UpdateWrapper<DeviceCommand>()
                    .set("status", status)
                    .set("result_message", request.getResultMessage())
                    .set("finish_time", TimeUtils.now())
                    .eq("id", commandId)
                    .eq("status", CommonConstant.COMMAND_PENDING));
            if (updated == 1) {
                return toVO(deviceCommandMapper.selectById(commandId));
            }
            // 并发下被其它请求抢先更新，重新读回走已完成分支
            command = deviceCommandMapper.selectById(commandId);
        }

        // 已完成分支：相同结果幂等返回；相反结果拒绝覆盖
        if (command.getStatus().equals(status)) {
            return toVO(command);
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "指令已完成，不允许覆盖为相反结果");
    }

    /** 创建 PENDING 指令（source 已由调用方校验） */
    private DeviceCommandVO createCommand(String deviceCode, String action, String source) {
        DeviceCommand command = new DeviceCommand();
        command.setCommandCode(generateCommandCode());
        command.setDeviceCode(deviceCode);
        command.setAction(action);
        command.setSource(source);
        command.setStatus(CommonConstant.COMMAND_PENDING);
        command.setCreateTime(TimeUtils.now());
        deviceCommandMapper.insert(command);
        return toVO(command);
    }

    private void requireDeviceExists(String deviceCode) {
        Long count = deviceMapper.selectCount(new QueryWrapper<Device>().eq("device_code", deviceCode));
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "设备不存在: " + deviceCode);
        }
    }

    private void requireValidAction(String action) {
        if (!CommonConstant.ACTION_TURN_ON.equals(action) && !CommonConstant.ACTION_TURN_OFF.equals(action)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "不支持的指令动作: " + action + "（仅支持 TURN_ON/TURN_OFF）");
        }
    }

    private String generateCommandCode() {
        return "CMD" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    static DeviceCommandVO toVO(DeviceCommand entity) {
        DeviceCommandVO vo = new DeviceCommandVO();
        vo.setId(entity.getId());
        vo.setCommandCode(entity.getCommandCode());
        vo.setDeviceCode(entity.getDeviceCode());
        vo.setAction(entity.getAction());
        vo.setSource(entity.getSource());
        vo.setStatus(entity.getStatus());
        vo.setResultMessage(entity.getResultMessage());
        vo.setCreateTime(TimeUtils.toEpochMillis(entity.getCreateTime()));
        vo.setFinishTime(entity.getFinishTime() == null ? null : TimeUtils.toEpochMillis(entity.getFinishTime()));
        return vo;
    }
}

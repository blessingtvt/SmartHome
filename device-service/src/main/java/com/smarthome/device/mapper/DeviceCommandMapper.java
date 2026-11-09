package com.smarthome.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthome.device.entity.DeviceCommand;
import org.apache.ibatis.annotations.Mapper;

/**
 * device_command 表 Mapper（归属 S3）。
 */
@Mapper
public interface DeviceCommandMapper extends BaseMapper<DeviceCommand> {
}

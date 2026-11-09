package com.smarthome.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthome.device.entity.Device;
import org.apache.ibatis.annotations.Mapper;

/**
 * device 表 Mapper（归属 S3）。
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}

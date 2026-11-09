package com.smarthome.environment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthome.environment.entity.EnvironmentData;
import org.apache.ibatis.annotations.Mapper;

/**
 * environment_data 表 Mapper（归属 S1）。
 */
@Mapper
public interface EnvironmentDataMapper extends BaseMapper<EnvironmentData> {
}

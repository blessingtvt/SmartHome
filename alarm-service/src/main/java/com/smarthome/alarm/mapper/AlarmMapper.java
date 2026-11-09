package com.smarthome.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthome.alarm.entity.Alarm;
import org.apache.ibatis.annotations.Mapper;

/**
 * alarm 表 Mapper（归属 S4）。
 */
@Mapper
public interface AlarmMapper extends BaseMapper<Alarm> {
}

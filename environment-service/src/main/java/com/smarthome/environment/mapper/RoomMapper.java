package com.smarthome.environment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthome.environment.entity.Room;
import org.apache.ibatis.annotations.Mapper;

/**
 * room 表 Mapper（归属 S1）。
 */
@Mapper
public interface RoomMapper extends BaseMapper<Room> {
}

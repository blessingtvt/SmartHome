package com.smarthome.environment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthome.environment.entity.Family;
import org.apache.ibatis.annotations.Mapper;

/**
 * family 表 Mapper（归属 S1）。
 */
@Mapper
public interface FamilyMapper extends BaseMapper<Family> {
}

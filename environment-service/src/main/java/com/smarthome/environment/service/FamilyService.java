package com.smarthome.environment.service;

import com.smarthome.common.exception.BusinessException;
import com.smarthome.common.result.ResultCode;
import com.smarthome.common.utils.TimeUtils;
import com.smarthome.environment.dto.FamilyRequest;
import com.smarthome.environment.entity.Family;
import com.smarthome.environment.mapper.FamilyMapper;
import com.smarthome.environment.vo.FamilyVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 家庭信息业务逻辑（S1）。
 */
@Service
public class FamilyService {

    private final FamilyMapper familyMapper;

    public FamilyService(FamilyMapper familyMapper) {
        this.familyMapper = familyMapper;
    }

    public FamilyVO create(FamilyRequest request) {
        Family entity = new Family();
        entity.setFamilyName(request.getFamilyName());
        entity.setAddress(request.getAddress());
        entity.setCreateTime(TimeUtils.now());
        familyMapper.insert(entity);
        return toVO(entity);
    }

    public List<FamilyVO> list() {
        return familyMapper.selectList(null).stream()
                .map(FamilyService::toVO)
                .toList();
    }

    public FamilyVO getById(Long id) {
        Family entity = requireEntity(id);
        return toVO(entity);
    }

    public FamilyVO update(Long id, FamilyRequest request) {
        Family entity = requireEntity(id);
        entity.setFamilyName(request.getFamilyName());
        entity.setAddress(request.getAddress());
        familyMapper.updateById(entity);
        return toVO(entity);
    }

    public void delete(Long id) {
        requireEntity(id);
        familyMapper.deleteById(id);
    }

    private Family requireEntity(Long id) {
        Family entity = familyMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "家庭不存在: id=" + id);
        }
        return entity;
    }

    static FamilyVO toVO(Family entity) {
        FamilyVO vo = new FamilyVO();
        vo.setId(entity.getId());
        vo.setFamilyName(entity.getFamilyName());
        vo.setAddress(entity.getAddress());
        vo.setCreateTime(TimeUtils.toEpochMillis(entity.getCreateTime()));
        return vo;
    }
}

package cn.ezios.baseapi.gcan.fault.service.impl;

import cn.ezios.baseapi.common.enums.ResponseCode;
import cn.ezios.baseapi.common.exception.BusinessException;
import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.fault.dto.FaultProfilePageQuery;
import cn.ezios.baseapi.gcan.fault.dto.FaultProfileSaveRequest;
import cn.ezios.baseapi.gcan.fault.entity.GcanFaultDefinition;
import cn.ezios.baseapi.gcan.fault.entity.GcanFaultProfile;
import cn.ezios.baseapi.gcan.fault.mapper.GcanFaultDefinitionMapper;
import cn.ezios.baseapi.gcan.fault.mapper.GcanFaultProfileMapper;
import cn.ezios.baseapi.gcan.fault.service.FaultProfileService;
import cn.ezios.baseapi.gcan.fault.vo.FaultProfileVO;
import cn.ezios.baseapi.gcan.fault.vo.FaultResultVO;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.vehicle.mapper.GcanVehicleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FaultProfileServiceImpl implements FaultProfileService {

    private static final int STATUS_ENABLED = 1;

    private final GcanFaultProfileMapper profileMapper;
    private final GcanFaultDefinitionMapper definitionMapper;
    private final GcanVehicleMapper vehicleMapper;

    public FaultProfileServiceImpl(GcanFaultProfileMapper profileMapper,
                                   GcanFaultDefinitionMapper definitionMapper,
                                   GcanVehicleMapper vehicleMapper) {
        this.profileMapper = profileMapper;
        this.definitionMapper = definitionMapper;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    public PageResult<FaultProfileVO> page(FaultProfilePageQuery query) {
        Page<GcanFaultProfile> page = profileMapper.selectPage(Page.of(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<GcanFaultProfile>()
                        .like(StringUtils.hasText(query.getProfileCode()), GcanFaultProfile::getProfileCode, query.getProfileCode())
                        .like(StringUtils.hasText(query.getProfileName()), GcanFaultProfile::getProfileName, query.getProfileName())
                        .like(StringUtils.hasText(query.getManufacturer()), GcanFaultProfile::getManufacturer, query.getManufacturer())
                        .eq(StringUtils.hasText(query.getVehicleType()), GcanFaultProfile::getVehicleType, query.getVehicleType())
                        .eq(query.getStatus() != null, GcanFaultProfile::getStatus, query.getStatus())
                        .orderByDesc(GcanFaultProfile::getId));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public List<FaultProfileVO> listEnabled() {
        return profileMapper.selectList(new LambdaQueryWrapper<GcanFaultProfile>()
                        .eq(GcanFaultProfile::getStatus, STATUS_ENABLED)
                        .orderByDesc(GcanFaultProfile::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public FaultProfileVO getDetail(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(FaultProfileSaveRequest request) {
        String profileCode = normalize(request.getProfileCode());
        ensureCodeUnique(profileCode, null);
        GcanFaultProfile profile = new GcanFaultProfile();
        BeanUtils.copyProperties(request, profile);
        profile.setProfileCode(profileCode);
        profile.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
        try {
            profileMapper.insert(profile);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("故障码表编码已存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FaultProfileSaveRequest request) {
        GcanFaultProfile existing = requireById(id);
        String profileCode = normalize(request.getProfileCode());
        if (!Objects.equals(existing.getProfileCode(), profileCode)) {
            throw new BusinessException("故障码表编码创建后不可修改");
        }
        GcanFaultProfile profile = new GcanFaultProfile();
        BeanUtils.copyProperties(request, profile);
        profile.setId(id);
        profile.setProfileCode(existing.getProfileCode());
        if (request.getStatus() == null) {
            profile.setStatus(existing.getStatus());
        }
        profileMapper.updateById(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GcanFaultProfile profile = requireById(id);
        if (isUsed(profile.getProfileCode())) {
            throw new BusinessException("正在被车辆使用的故障码表不允许删除");
        }
        profileMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(BatchIdsRequest request) {
        for (Long id : request.getIds()) {
            delete(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, StatusUpdateRequest request) {
        requireById(id);
        GcanFaultProfile profile = new GcanFaultProfile();
        profile.setId(id);
        profile.setStatus(request.getStatus());
        profileMapper.updateById(profile);
    }

    @Override
    public GcanFaultProfile requireByCode(String profileCode) {
        GcanFaultProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<GcanFaultProfile>()
                .eq(GcanFaultProfile::getProfileCode, normalize(profileCode)));
        if (profile == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND);
        }
        return profile;
    }

    @Override
    public void requireSelectable(String profileCode) {
        GcanFaultProfile profile = requireByCode(profileCode);
        if (!Objects.equals(profile.getStatus(), STATUS_ENABLED)) {
            throw new BusinessException("停用的故障码表不能关联新车辆");
        }
    }

    @Override
    public FaultResultVO resolve(String profileCode, String faultCode) {
        FaultResultVO result = new FaultResultVO();
        result.setFaultProfileCode(normalize(profileCode));
        result.setFaultCode(normalize(faultCode));
        if (!StringUtils.hasText(faultCode)) {
            result.setStatus(FaultResultVO.UNKNOWN_FAULT);
            return result;
        }
        if ("0".equals(result.getFaultCode())) {
            result.setStatus(FaultResultVO.NO_FAULT);
            return result;
        }
        if (!StringUtils.hasText(profileCode)) {
            result.setStatus(FaultResultVO.UNCONFIGURED_PROFILE);
            return result;
        }
        GcanFaultProfile profile = profileMapper.selectOne(new LambdaQueryWrapper<GcanFaultProfile>()
                .eq(GcanFaultProfile::getProfileCode, result.getFaultProfileCode()));
        if (profile == null) {
            result.setStatus(FaultResultVO.UNCONFIGURED_PROFILE);
            return result;
        }
        GcanFaultDefinition definition = definitionMapper.selectOne(new LambdaQueryWrapper<GcanFaultDefinition>()
                .eq(GcanFaultDefinition::getProfileCode, result.getFaultProfileCode())
                .eq(GcanFaultDefinition::getFaultCode, result.getFaultCode())
                .eq(GcanFaultDefinition::getStatus, STATUS_ENABLED));
        if (definition == null) {
            result.setStatus(FaultResultVO.UNKNOWN_FAULT);
            return result;
        }
        result.setStatus(FaultResultVO.MATCHED);
        BeanUtils.copyProperties(definition, result);
        result.setFaultProfileCode(definition.getProfileCode());
        return result;
    }

    private GcanFaultProfile requireById(Long id) {
        GcanFaultProfile profile = profileMapper.selectById(id);
        if (profile == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND);
        }
        return profile;
    }

    private void ensureCodeUnique(String profileCode, Long excludeId) {
        Long count = profileMapper.selectCount(new LambdaQueryWrapper<GcanFaultProfile>()
                .eq(GcanFaultProfile::getProfileCode, profileCode)
                .ne(excludeId != null, GcanFaultProfile::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("故障码表编码已存在");
        }
    }

    private boolean isUsed(String profileCode) {
        return vehicleMapper.selectCount(new LambdaQueryWrapper<GcanVehicle>()
                .eq(GcanVehicle::getFaultProfileCode, profileCode)) > 0;
    }

    private FaultProfileVO toVO(GcanFaultProfile profile) {
        FaultProfileVO vo = new FaultProfileVO();
        BeanUtils.copyProperties(profile, vo);
        return vo;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

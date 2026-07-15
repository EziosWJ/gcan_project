package cn.ezios.baseapi.gcan.fault.service.impl;

import cn.ezios.baseapi.common.enums.ResponseCode;
import cn.ezios.baseapi.common.exception.BusinessException;
import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.fault.dto.FaultDefinitionPageQuery;
import cn.ezios.baseapi.gcan.fault.dto.FaultDefinitionSaveRequest;
import cn.ezios.baseapi.gcan.fault.entity.GcanFaultDefinition;
import cn.ezios.baseapi.gcan.fault.entity.GcanFaultProfile;
import cn.ezios.baseapi.gcan.fault.mapper.GcanFaultDefinitionMapper;
import cn.ezios.baseapi.gcan.fault.mapper.GcanFaultProfileMapper;
import cn.ezios.baseapi.gcan.fault.service.FaultDefinitionService;
import cn.ezios.baseapi.gcan.fault.vo.FaultDefinitionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FaultDefinitionServiceImpl implements FaultDefinitionService {

    private static final int STATUS_ENABLED = 1;

    private final GcanFaultDefinitionMapper definitionMapper;
    private final GcanFaultProfileMapper profileMapper;

    public FaultDefinitionServiceImpl(GcanFaultDefinitionMapper definitionMapper,
                                      GcanFaultProfileMapper profileMapper) {
        this.definitionMapper = definitionMapper;
        this.profileMapper = profileMapper;
    }

    @Override
    public PageResult<FaultDefinitionVO> page(FaultDefinitionPageQuery query) {
        Page<GcanFaultDefinition> page = definitionMapper.selectPage(Page.of(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<GcanFaultDefinition>()
                        .eq(StringUtils.hasText(query.getProfileCode()), GcanFaultDefinition::getProfileCode, normalize(query.getProfileCode()))
                        .like(StringUtils.hasText(query.getFaultCode()), GcanFaultDefinition::getFaultCode, query.getFaultCode())
                        .like(StringUtils.hasText(query.getFaultName()), GcanFaultDefinition::getFaultName, query.getFaultName())
                        .eq(query.getStatus() != null, GcanFaultDefinition::getStatus, query.getStatus())
                        .orderByDesc(GcanFaultDefinition::getId));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public FaultDefinitionVO getDetail(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(FaultDefinitionSaveRequest request) {
        String profileCode = normalize(request.getProfileCode());
        String faultCode = normalize(request.getFaultCode());
        requireProfile(profileCode);
        ensureCodeUnique(profileCode, faultCode, null);
        GcanFaultDefinition definition = new GcanFaultDefinition();
        BeanUtils.copyProperties(request, definition);
        definition.setProfileCode(profileCode);
        definition.setFaultCode(faultCode);
        definition.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
        try {
            definitionMapper.insert(definition);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("故障码在该故障码表中已存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FaultDefinitionSaveRequest request) {
        GcanFaultDefinition existing = requireById(id);
        String profileCode = normalize(request.getProfileCode());
        String faultCode = normalize(request.getFaultCode());
        requireProfile(profileCode);
        ensureCodeUnique(profileCode, faultCode, id);
        GcanFaultDefinition definition = new GcanFaultDefinition();
        BeanUtils.copyProperties(request, definition);
        definition.setId(id);
        definition.setProfileCode(profileCode);
        definition.setFaultCode(faultCode);
        if (request.getStatus() == null) {
            definition.setStatus(existing.getStatus());
        }
        definitionMapper.updateById(definition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        definitionMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(BatchIdsRequest request) {
        for (Long id : request.getIds()) {
            if (definitionMapper.selectById(id) != null) {
                definitionMapper.deleteById(id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, StatusUpdateRequest request) {
        requireById(id);
        GcanFaultDefinition definition = new GcanFaultDefinition();
        definition.setId(id);
        definition.setStatus(request.getStatus());
        definitionMapper.updateById(definition);
    }

    private GcanFaultDefinition requireById(Long id) {
        GcanFaultDefinition definition = definitionMapper.selectById(id);
        if (definition == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND);
        }
        return definition;
    }

    private void requireProfile(String profileCode) {
        if (profileMapper.selectOne(new LambdaQueryWrapper<GcanFaultProfile>()
                .eq(GcanFaultProfile::getProfileCode, profileCode)) == null) {
            throw new BusinessException("故障码表不存在");
        }
    }

    private void ensureCodeUnique(String profileCode, String faultCode, Long excludeId) {
        Long count = definitionMapper.selectCount(new LambdaQueryWrapper<GcanFaultDefinition>()
                .eq(GcanFaultDefinition::getProfileCode, profileCode)
                .eq(GcanFaultDefinition::getFaultCode, faultCode)
                .ne(excludeId != null, GcanFaultDefinition::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("故障码在该故障码表中已存在");
        }
    }

    private FaultDefinitionVO toVO(GcanFaultDefinition definition) {
        FaultDefinitionVO vo = new FaultDefinitionVO();
        BeanUtils.copyProperties(definition, vo);
        return vo;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

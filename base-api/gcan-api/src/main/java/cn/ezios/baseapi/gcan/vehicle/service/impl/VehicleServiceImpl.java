package cn.ezios.baseapi.gcan.vehicle.service.impl;

import cn.ezios.baseapi.common.enums.ResponseCode;
import cn.ezios.baseapi.common.exception.BusinessException;
import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.common.BoxIdUtil;
import cn.ezios.baseapi.gcan.fault.entity.GcanFaultProfile;
import cn.ezios.baseapi.gcan.fault.service.FaultProfileService;
import cn.ezios.baseapi.gcan.external.ExternalMineNameStore;
import cn.ezios.baseapi.gcan.vehicle.dto.VehicleLookupQuery;
import cn.ezios.baseapi.gcan.vehicle.dto.VehiclePageQuery;
import cn.ezios.baseapi.gcan.vehicle.dto.VehicleSaveRequest;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.vehicle.mapper.GcanVehicleMapper;
import cn.ezios.baseapi.gcan.vehicle.service.VehicleService;
import cn.ezios.baseapi.gcan.vehicle.vo.VehicleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class VehicleServiceImpl implements VehicleService {

    private static final int STATUS_ENABLED = 1;

    private final GcanVehicleMapper vehicleMapper;
    private final FaultProfileService faultProfileService;
    private final ExternalMineNameStore externalMineNameStore;

    public VehicleServiceImpl(GcanVehicleMapper vehicleMapper, FaultProfileService faultProfileService,
                              ExternalMineNameStore externalMineNameStore) {
        this.vehicleMapper = vehicleMapper;
        this.faultProfileService = faultProfileService;
        this.externalMineNameStore = externalMineNameStore;
    }

    @Override
    public PageResult<VehicleVO> page(VehiclePageQuery query) {
        String boxIdHex = StringUtils.hasText(query.getBoxIdHex()) ? BoxIdUtil.normalizeHex(query.getBoxIdHex()) : null;
        Page<GcanVehicle> page = vehicleMapper.selectPage(Page.of(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<GcanVehicle>()
                        .like(StringUtils.hasText(query.getVehicleName()), GcanVehicle::getVehicleName, query.getVehicleName())
                        .eq(StringUtils.hasText(query.getMineId()), GcanVehicle::getMineId, query.getMineId())
                        .eq(StringUtils.hasText(query.getAccessMode()), GcanVehicle::getAccessMode, normalizeAccessMode(query.getAccessMode()))
                        .eq(StringUtils.hasText(query.getVehicleType()), GcanVehicle::getVehicleType, normalizeVehicleType(query.getVehicleType()))
                        .eq(StringUtils.hasText(boxIdHex), GcanVehicle::getBoxIdHex, boxIdHex)
                        .eq(query.getStatus() != null, GcanVehicle::getStatus, query.getStatus())
                        .orderByDesc(GcanVehicle::getId));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public List<VehicleVO> listEnabled() {
        return selectEnabled().stream().map(this::toVO).toList();
    }

    @Override
    public List<GcanVehicle> enabledVehicles(VehicleLookupQuery query) {
        return selectEnabled(query);
    }

    @Override
    public Map<String, GcanVehicle> enabledByBoxIdHex() {
        return enabledByBoxIdHex(new VehicleLookupQuery());
    }

    @Override
    public Map<String, GcanVehicle> enabledByBoxIdHex(VehicleLookupQuery query) {
        return selectEnabled(query).stream()
                .filter(this::isGcanVehicle)
                .filter(vehicle -> StringUtils.hasText(vehicle.getBoxIdHex()))
                .collect(Collectors.toMap(GcanVehicle::getBoxIdHex, Function.identity(), (a, b) -> a));
    }

    @Override
    public Map<String, GcanVehicle> byBoxIdHex(VehicleLookupQuery query) {
        return selectByLookup(query).stream()
                .filter(this::isGcanVehicle)
                .filter(vehicle -> StringUtils.hasText(vehicle.getBoxIdHex()))
                .collect(Collectors.toMap(GcanVehicle::getBoxIdHex, Function.identity(), (a, b) -> a));
    }

    @Override
    public Optional<GcanVehicle> findByExternalIdentity(String mineId, String externalVehicleCode) {
        return Optional.ofNullable(vehicleMapper.selectOne(new LambdaQueryWrapper<GcanVehicle>()
                .eq(GcanVehicle::getMineId, mineId)
                .eq(GcanVehicle::getExternalVehicleCode, externalVehicleCode)
                .eq(GcanVehicle::getAccessMode, "MINE_API")
                .last("LIMIT 1")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GcanVehicle createExternal(String mineId, String externalVehicleCode) {
        GcanVehicle vehicle = new GcanVehicle();
        vehicle.setVehicleName(mineId + "-" + externalVehicleCode);
        vehicle.setMineId(mineId);
        vehicle.setAccessMode("MINE_API");
        vehicle.setExternalVehicleCode(externalVehicleCode);
        vehicle.setVehicleType("EXTERNAL");
        vehicle.setStatus(STATUS_ENABLED);
        vehicleMapper.insert(vehicle);
        return vehicle;
    }

    @Override
    public VehicleVO getDetail(Long id) {
        return toVO(requireVehicle(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(VehicleSaveRequest request) {
        String accessMode = normalizeAccessMode(request.getAccessMode());
        String boxIdHex = normalizeBoxId(request.getBoxIdHex(), accessMode);
        String externalVehicleCode = normalizeExternalVehicleCode(request.getExternalVehicleCode(), accessMode);
        ensureBoxUnique(boxIdHex, null);
        ensureExternalIdentityUnique(request.getMineId().trim(), externalVehicleCode, null);
        String faultProfileCode = normalizeFaultProfileCode(request.getFaultProfileCode());
        validateFaultProfileCode(faultProfileCode, null);
        GcanVehicle vehicle = new GcanVehicle();
        BeanUtils.copyProperties(request, vehicle);
        vehicle.setMineId(request.getMineId().trim());
        vehicle.setAccessMode(accessMode);
        vehicle.setExternalVehicleCode(externalVehicleCode);
        vehicle.setVehicleType(normalizeVehicleType(request.getVehicleType()));
        vehicle.setFaultProfileCode(faultProfileCode);
        vehicle.setBoxIdHex(boxIdHex);
        vehicle.setBoxIdDec(boxIdHex == null ? null : BoxIdUtil.toDec(boxIdHex));
        vehicle.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
        vehicleMapper.insert(vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, VehicleSaveRequest request) {
        GcanVehicle existing = requireVehicle(id);
        String accessMode = normalizeAccessMode(request.getAccessMode());
        String boxIdHex = normalizeBoxId(request.getBoxIdHex(), accessMode);
        String externalVehicleCode = normalizeExternalVehicleCode(request.getExternalVehicleCode(), accessMode);
        ensureBoxUnique(boxIdHex, id);
        ensureExternalIdentityUnique(request.getMineId().trim(), externalVehicleCode, id);
        String faultProfileCode = normalizeFaultProfileCode(request.getFaultProfileCode());
        validateFaultProfileCode(faultProfileCode, existing);
        GcanVehicle vehicle = new GcanVehicle();
        BeanUtils.copyProperties(request, vehicle);
        vehicle.setId(id);
        vehicle.setMineId(request.getMineId().trim());
        vehicle.setAccessMode(accessMode);
        vehicle.setExternalVehicleCode(externalVehicleCode);
        vehicle.setVehicleType(normalizeVehicleType(request.getVehicleType()));
        vehicle.setFaultProfileCode(faultProfileCode);
        vehicle.setBoxIdHex(boxIdHex);
        vehicle.setBoxIdDec(boxIdHex == null ? null : BoxIdUtil.toDec(boxIdHex));
        vehicleMapper.updateById(vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireVehicle(id);
        vehicleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(BatchIdsRequest request) {
        for (Long id : request.getIds()) {
            if (vehicleMapper.selectById(id) != null) {
                vehicleMapper.deleteById(id);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, StatusUpdateRequest request) {
        requireVehicle(id);
        GcanVehicle vehicle = new GcanVehicle();
        vehicle.setId(id);
        vehicle.setStatus(request.getStatus());
        vehicleMapper.updateById(vehicle);
    }

    private List<GcanVehicle> selectEnabled() {
        return selectEnabled(new VehicleLookupQuery());
    }

    private List<GcanVehicle> selectEnabled(VehicleLookupQuery query) {
        return vehicleMapper.selectList(lookupWrapper(query)
                .eq(GcanVehicle::getStatus, STATUS_ENABLED));
    }

    private List<GcanVehicle> selectByLookup(VehicleLookupQuery query) {
        return vehicleMapper.selectList(lookupWrapper(query));
    }

    private LambdaQueryWrapper<GcanVehicle> lookupWrapper(VehicleLookupQuery query) {
        String boxIdHex = StringUtils.hasText(query.getBoxIdHex()) ? BoxIdUtil.normalizeHex(query.getBoxIdHex()) : null;
        return new LambdaQueryWrapper<GcanVehicle>()
                .like(StringUtils.hasText(query.getVehicleName()), GcanVehicle::getVehicleName, query.getVehicleName())
                .eq(StringUtils.hasText(query.getMineId()), GcanVehicle::getMineId, query.getMineId())
                .eq(StringUtils.hasText(query.getVehicleType()), GcanVehicle::getVehicleType, normalizeVehicleType(query.getVehicleType()))
                .eq(StringUtils.hasText(query.getExternalVehicleCode()), GcanVehicle::getExternalVehicleCode, query.getExternalVehicleCode())
                .eq(StringUtils.hasText(query.getAccessMode()), GcanVehicle::getAccessMode, normalizeAccessMode(query.getAccessMode()))
                .eq(StringUtils.hasText(boxIdHex), GcanVehicle::getBoxIdHex, boxIdHex)
                .orderByDesc(GcanVehicle::getId);
    }

    private GcanVehicle requireVehicle(Long id) {
        GcanVehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND);
        }
        return vehicle;
    }

    private void ensureBoxUnique(String boxIdHex, Long excludeId) {
        if (!StringUtils.hasText(boxIdHex)) {
            return;
        }
        Long count = vehicleMapper.selectCount(new LambdaQueryWrapper<GcanVehicle>()
                .eq(GcanVehicle::getBoxIdHex, boxIdHex)
                .ne(excludeId != null, GcanVehicle::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("盒子ID(HEX)已绑定车辆");
        }
    }

    private void ensureExternalIdentityUnique(String mineId, String externalVehicleCode, Long excludeId) {
        if (!StringUtils.hasText(externalVehicleCode)) {
            return;
        }
        Long count = vehicleMapper.selectCount(new LambdaQueryWrapper<GcanVehicle>()
                .eq(GcanVehicle::getMineId, mineId)
                .eq(GcanVehicle::getExternalVehicleCode, externalVehicleCode)
                .eq(GcanVehicle::getAccessMode, "MINE_API")
                .ne(excludeId != null, GcanVehicle::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("外部车辆编码已绑定车辆");
        }
    }

    private VehicleVO toVO(GcanVehicle vehicle) {
        VehicleVO vo = new VehicleVO();
        BeanUtils.copyProperties(vehicle, vo);
        if ("MINE_API".equalsIgnoreCase(vehicle.getAccessMode())) {
            vo.setMineName(externalMineNameStore.name(vehicle.getMineId()));
        }
        vo.setVehicleTypeLabel(vehicle.getVehicleType());
        return vo;
    }

    private String normalizeVehicleType(String vehicleType) {
        return vehicleType == null ? null : vehicleType.trim().toUpperCase(Locale.ROOT);
    }

    private void validateFaultProfileCode(String profileCode, GcanVehicle existing) {
        if (!StringUtils.hasText(profileCode)) {
            return;
        }
        GcanFaultProfile profile = faultProfileService.requireByCode(profileCode);
        if (!Objects.equals(profile.getStatus(), STATUS_ENABLED)
                && (existing == null || !Objects.equals(existing.getFaultProfileCode(), profileCode))) {
            throw new BusinessException("停用的故障码表不能关联新车辆");
        }
    }

    private String normalizeFaultProfileCode(String profileCode) {
        return StringUtils.hasText(profileCode) ? profileCode.trim() : null;
    }

    private String normalizeAccessMode(String accessMode) {
        return StringUtils.hasText(accessMode) ? accessMode.trim().toUpperCase(Locale.ROOT) : "GCAN";
    }

    private String normalizeBoxId(String boxIdHex, String accessMode) {
        if ("MINE_API".equals(accessMode)) {
            return null;
        }
        if (!StringUtils.hasText(boxIdHex)) {
            throw new BusinessException("GCAN车辆必须绑定盒子ID(HEX)");
        }
        return BoxIdUtil.normalizeHex(boxIdHex);
    }

    private String normalizeExternalVehicleCode(String code, String accessMode) {
        if (!"MINE_API".equals(accessMode)) {
            return null;
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("外部车辆必须填写外部车辆编码");
        }
        return code.trim();
    }

    private boolean isGcanVehicle(GcanVehicle vehicle) {
        return vehicle.getAccessMode() == null || "GCAN".equalsIgnoreCase(vehicle.getAccessMode());
    }
}

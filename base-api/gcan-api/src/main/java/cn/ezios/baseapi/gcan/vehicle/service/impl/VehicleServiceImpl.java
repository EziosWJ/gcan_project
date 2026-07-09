package cn.ezios.baseapi.gcan.vehicle.service.impl;

import cn.ezios.baseapi.common.enums.ResponseCode;
import cn.ezios.baseapi.common.exception.BusinessException;
import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.common.BoxIdUtil;
import cn.ezios.baseapi.gcan.vehicle.VehicleType;
import cn.ezios.baseapi.gcan.vehicle.dto.VehiclePageQuery;
import cn.ezios.baseapi.gcan.vehicle.dto.VehicleSaveRequest;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.vehicle.mapper.GcanVehicleMapper;
import cn.ezios.baseapi.gcan.vehicle.service.VehicleService;
import cn.ezios.baseapi.gcan.vehicle.vo.VehicleTypeVO;
import cn.ezios.baseapi.gcan.vehicle.vo.VehicleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    public VehicleServiceImpl(GcanVehicleMapper vehicleMapper) {
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    public PageResult<VehicleVO> page(VehiclePageQuery query) {
        String boxIdHex = StringUtils.hasText(query.getBoxIdHex()) ? BoxIdUtil.normalizeHex(query.getBoxIdHex()) : null;
        Page<GcanVehicle> page = vehicleMapper.selectPage(Page.of(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<GcanVehicle>()
                        .like(StringUtils.hasText(query.getVehicleName()), GcanVehicle::getVehicleName, query.getVehicleName())
                        .eq(StringUtils.hasText(query.getVehicleType()), GcanVehicle::getVehicleType, query.getVehicleType())
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
    public Map<String, GcanVehicle> enabledByBoxIdHex() {
        return selectEnabled().stream().collect(Collectors.toMap(GcanVehicle::getBoxIdHex, Function.identity(), (a, b) -> a));
    }

    @Override
    public VehicleVO getDetail(Long id) {
        return toVO(requireVehicle(id));
    }

    @Override
    public List<VehicleTypeVO> vehicleTypes() {
        return Arrays.stream(VehicleType.values())
                .map(type -> new VehicleTypeVO(type.getCode(), type.getLabel()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(VehicleSaveRequest request) {
        VehicleType.requireValid(request.getVehicleType());
        String boxIdHex = BoxIdUtil.normalizeHex(request.getBoxIdHex());
        ensureBoxUnique(boxIdHex, null);
        GcanVehicle vehicle = new GcanVehicle();
        BeanUtils.copyProperties(request, vehicle);
        vehicle.setBoxIdHex(boxIdHex);
        vehicle.setBoxIdDec(BoxIdUtil.toDec(boxIdHex));
        vehicle.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
        vehicleMapper.insert(vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, VehicleSaveRequest request) {
        requireVehicle(id);
        VehicleType.requireValid(request.getVehicleType());
        String boxIdHex = BoxIdUtil.normalizeHex(request.getBoxIdHex());
        ensureBoxUnique(boxIdHex, id);
        GcanVehicle vehicle = new GcanVehicle();
        BeanUtils.copyProperties(request, vehicle);
        vehicle.setId(id);
        vehicle.setBoxIdHex(boxIdHex);
        vehicle.setBoxIdDec(BoxIdUtil.toDec(boxIdHex));
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
        return vehicleMapper.selectList(new LambdaQueryWrapper<GcanVehicle>()
                .eq(GcanVehicle::getStatus, STATUS_ENABLED)
                .orderByDesc(GcanVehicle::getId));
    }

    private GcanVehicle requireVehicle(Long id) {
        GcanVehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND);
        }
        return vehicle;
    }

    private void ensureBoxUnique(String boxIdHex, Long excludeId) {
        Long count = vehicleMapper.selectCount(new LambdaQueryWrapper<GcanVehicle>()
                .eq(GcanVehicle::getBoxIdHex, boxIdHex)
                .ne(excludeId != null, GcanVehicle::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("盒子ID(HEX)已绑定车辆");
        }
    }

    private VehicleVO toVO(GcanVehicle vehicle) {
        VehicleVO vo = new VehicleVO();
        BeanUtils.copyProperties(vehicle, vo);
        try {
            vo.setVehicleTypeLabel(VehicleType.valueOf(vehicle.getVehicleType()).getLabel());
        } catch (IllegalArgumentException e) {
            vo.setVehicleTypeLabel(vehicle.getVehicleType());
        }
        return vo;
    }
}

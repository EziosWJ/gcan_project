package cn.ezios.baseapi.gcan.vehicle.service;

import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.vehicle.dto.VehiclePageQuery;
import cn.ezios.baseapi.gcan.vehicle.dto.VehicleLookupQuery;
import cn.ezios.baseapi.gcan.vehicle.dto.VehicleSaveRequest;
import cn.ezios.baseapi.gcan.vehicle.entity.GcanVehicle;
import cn.ezios.baseapi.gcan.vehicle.vo.VehicleVO;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface VehicleService {

    PageResult<VehicleVO> page(VehiclePageQuery query);

    List<VehicleVO> listEnabled();

    List<GcanVehicle> enabledVehicles(VehicleLookupQuery query);

    Map<String, GcanVehicle> enabledByBoxIdHex();

    Map<String, GcanVehicle> enabledByBoxIdHex(VehicleLookupQuery query);

    Map<String, GcanVehicle> byBoxIdHex(VehicleLookupQuery query);

    Optional<GcanVehicle> findByExternalIdentity(String mineId, String externalVehicleCode);

    GcanVehicle createExternal(String mineId, String externalVehicleCode);

    VehicleVO getDetail(Long id);

    void create(VehicleSaveRequest request);

    void update(Long id, VehicleSaveRequest request);

    void delete(Long id);

    void deleteBatch(BatchIdsRequest request);

    void updateStatus(Long id, StatusUpdateRequest request);
}

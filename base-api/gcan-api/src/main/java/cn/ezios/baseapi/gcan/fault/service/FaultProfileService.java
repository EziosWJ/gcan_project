package cn.ezios.baseapi.gcan.fault.service;

import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.fault.dto.FaultProfilePageQuery;
import cn.ezios.baseapi.gcan.fault.dto.FaultProfileSaveRequest;
import cn.ezios.baseapi.gcan.fault.entity.GcanFaultProfile;
import cn.ezios.baseapi.gcan.fault.vo.FaultProfileVO;
import cn.ezios.baseapi.gcan.fault.vo.FaultResultVO;
import java.util.List;

public interface FaultProfileService {

    PageResult<FaultProfileVO> page(FaultProfilePageQuery query);

    List<FaultProfileVO> listEnabled();

    FaultProfileVO getDetail(Long id);

    void create(FaultProfileSaveRequest request);

    void update(Long id, FaultProfileSaveRequest request);

    void delete(Long id);

    void deleteBatch(BatchIdsRequest request);

    void updateStatus(Long id, StatusUpdateRequest request);

    GcanFaultProfile requireByCode(String profileCode);

    void requireSelectable(String profileCode);

    FaultResultVO resolve(String profileCode, String faultCode);
}

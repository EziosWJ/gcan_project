package cn.ezios.baseapi.gcan.fault.service;

import cn.ezios.baseapi.common.model.BatchIdsRequest;
import cn.ezios.baseapi.common.model.PageResult;
import cn.ezios.baseapi.common.model.StatusUpdateRequest;
import cn.ezios.baseapi.gcan.fault.dto.FaultDefinitionPageQuery;
import cn.ezios.baseapi.gcan.fault.dto.FaultDefinitionSaveRequest;
import cn.ezios.baseapi.gcan.fault.vo.FaultDefinitionVO;

public interface FaultDefinitionService {

    PageResult<FaultDefinitionVO> page(FaultDefinitionPageQuery query);

    FaultDefinitionVO getDetail(Long id);

    void create(FaultDefinitionSaveRequest request);

    void update(Long id, FaultDefinitionSaveRequest request);

    void delete(Long id);

    void deleteBatch(BatchIdsRequest request);

    void updateStatus(Long id, StatusUpdateRequest request);
}

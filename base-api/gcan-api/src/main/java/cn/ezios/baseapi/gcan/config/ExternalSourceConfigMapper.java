package cn.ezios.baseapi.gcan.config;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExternalSourceConfigMapper {

    @Select("""
            SELECT config_key, config_value
            FROM sys_config
            WHERE deleted = 0
              AND status = 1
              AND config_key LIKE 'gcan.external.%'
            """)
    List<ExternalSourceConfigRow> selectActive();
}

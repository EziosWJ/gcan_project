package cn.ezios.baseapi.gcan.vehicle;

import cn.ezios.baseapi.common.exception.BusinessException;
import java.util.Arrays;

public enum VehicleType {
    REN_19("19座人车"),
    REN_19_B("19座人车B型"),
    LIAO_1_9T("1.9T料车"),
    LIAO_5T("5T料车");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static void requireValid(String code) {
        boolean valid = Arrays.stream(values()).anyMatch(type -> type.name().equals(code));
        if (!valid) {
            throw new BusinessException("车辆类型不支持");
        }
    }
}

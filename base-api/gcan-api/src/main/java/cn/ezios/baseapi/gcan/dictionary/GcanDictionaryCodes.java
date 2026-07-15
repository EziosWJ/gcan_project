package cn.ezios.baseapi.gcan.dictionary;

import java.util.List;

public final class GcanDictionaryCodes {

    public static final String MINE = "gcan_mine";
    public static final String VEHICLE_TYPE = "gcan_vehicle_type";
    public static final String VEHICLE_CONNECTION_STATUS = "gcan_vehicle_connection_status";
    public static final String VEHICLE_PARSE_STATUS = "gcan_vehicle_parse_status";

    public static final List<String> ALL = List.of(
            MINE,
            VEHICLE_TYPE,
            VEHICLE_CONNECTION_STATUS,
            VEHICLE_PARSE_STATUS);

    private GcanDictionaryCodes() {
    }
}

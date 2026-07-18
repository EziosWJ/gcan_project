package cn.ezios.baseapi.gcan.raw.mirror;

import cn.ezios.baseapi.gcan.raw.RawCanFrame;
import java.util.List;

public interface GcanRawFrameSource {

    List<RawCanFrame> load(String boxIdHex);
}

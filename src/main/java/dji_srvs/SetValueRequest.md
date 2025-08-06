package dji_srvs;

import org.ros.internal.message.Message;

public interface SetValueRequest extends Message {
    String _TYPE = "dji_srvs/SetValueRequest";
    String _DEFINITION = "float32 data # e.g. for setting values for MSDK API\n";

    float getData();

    void setData(float var1);
}

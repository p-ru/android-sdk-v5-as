package dji_srvs;

import org.ros.internal.message.Message;

public interface SetValue extends Message {
    String _TYPE = "dji_srvs/SetValue";
    String _DEFINITION = "float32 data # e.g. for setting values for MSDK API\n---\nbool success   # indicate successful run of set value\nstring message # informational, e.g. for error messages\n";
}



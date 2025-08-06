package dji_srvs;

import org.ros.internal.message.Message;

public interface SetValueResponse extends Message {
    String _TYPE = "dji_srvs/SetValueResponse";
    String _DEFINITION = "bool success   # indicate successful run of triggered service\nstring message # informational, e.g. for error messages";

    boolean getSuccess();

    void setSuccess(boolean var1);

    String getMessage();

    void setMessage(String var1);
}

package core.network.http.proto;

import com.google.gson.JsonObject;

public class WXTransfer_Result {
    String return_code;
    String err_code;
    String return_msg;


    public static String make(String return_code, String err_code, String return_msg) {
        JsonObject rtn = new JsonObject();
        rtn.addProperty("return_code", return_code);
        rtn.addProperty("err_code", err_code);
        rtn.addProperty("return_msg", return_msg);
        return rtn.toString();
    }
}

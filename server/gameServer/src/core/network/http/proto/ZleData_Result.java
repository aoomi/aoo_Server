package core.network.http.proto;

import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;

import java.io.Serializable;

public class ZleData_Result<T> implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public int code;
    public T data;
    public String msg;


    public ZleData_Result() {
        super();
    }

    public ZleData_Result(int code, String msg) {
        super();
        this.code = code;
        this.msg = msg;
    }

    public ZleData_Result(int code, T data, String msg) {
        super();
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public static <T> String make(ErrorCode code, T data) {
        ZleData_Result<T> rtn = new ZleData_Result<T>();
        rtn.setCode(code.value());
        rtn.setData(data);
        return rtn.toJson(ZleData_Result.class);
    }


    public static String make(ErrorCode code, String msg) {
        ZleData_Result<?> rtn = new ZleData_Result<>();
        rtn.setCode(code.value());
        rtn.setMsg(msg);
        return rtn.toJson(ZleData_Result.class);
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @SuppressWarnings("hiding")
    public <T> String toJson(Class<T> clazz) {
        Gson gson = new Gson();
        return gson.toJson(this, ZleData_Result.class);
    }

    public static ZleData_Result<?> fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, ZleData_Result.class);
    }
}

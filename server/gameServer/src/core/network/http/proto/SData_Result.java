package core.network.http.proto;

import com.ddm.server.websocket.def.ErrorCode;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

@Data
public class SData_Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private ErrorCode code;
    private T data;
    private String msg;
    private long custom;

    public static <T> SData_Result<T> make(ErrorCode code) {
        SData_Result<T> rtn = new SData_Result<T>();
        rtn.setCode(code);
        rtn.setMsg("");
        return rtn;
    }

    public static <T> SData_Result<T> make(ErrorCode code, int custom) {
        SData_Result<T> rtn = new SData_Result<T>();
        rtn.setCode(code);
        rtn.setCustom(custom);
        return rtn;
    }

    public static <T> SData_Result<T> make(ErrorCode code, String format, Object... params) {
        SData_Result<T> rtn = new SData_Result<T>();
        rtn.setCode(code);
        rtn.setMsg(String.format(format, params));
        return rtn;
    }

    public static <T> SData_Result<T> make(ErrorCode code, String format) {
        SData_Result<T> rtn = new SData_Result<T>();
        rtn.setCode(code);
        rtn.setMsg(format);
        return rtn;
    }

    public static <T> SData_Result<T> make(ErrorCode code, T data) {
        SData_Result<T> rtn = new SData_Result<T>();
        rtn.setCode(code);
        rtn.setData(data);
        return rtn;
    }

    public static <T> SData_Result<T> make(ErrorCode code, long custom) {
        SData_Result<T> rtn = new SData_Result<T>();
        rtn.setCode(code);
        rtn.setCustom(custom);
        return rtn;
    }

//	public static <T>SData_Result<T> make(ErrorCode code,T data,String format) {
//		SData_Result<T> rtn = new SData_Result<T>();
//		rtn.setCode(code);
//		rtn.setData(data);
//		rtn.setMsg(format);
//		return rtn;
//	}

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}

package jsproto.c2s;

import java.io.Serializable;
import java.util.LinkedHashMap;

import com.ddm.server.common.utils.CommLogD;
import com.ddm.server.common.utils.GsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import lombok.Data;

@Data
public class SData_Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private int codeInt;
    private String dataClassName;
    private T data;
    private String msg;
    private double custom;



    public String toJson() {
        return GsonUtils.toJsonString(this);
    }

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

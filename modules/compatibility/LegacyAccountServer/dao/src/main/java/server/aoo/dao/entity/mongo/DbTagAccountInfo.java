package server.aoo.dao.entity.mongo;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Builder
@Document(collection = "DbTagAccountInfo")
@CompoundIndexes({@CompoundIndex(name = "stateLoginTime", def = "{'accountState':1,'loginTime':-1}")})
public class DbTagAccountInfo implements Serializable {
    /**
     * id
     */
    @Id
    private long id;
    /**
     * 登录ip列表
     */
    private List<String> loginIPList;
    /**
     * 注册ip
     */
    private String registerIP;
    /*
     * 注册时间
     */
    private Date registerTime;
    /**
     * 账号状态
     */
    private int accountState;
    /**
     * 登录时间
     */
    private long loginTime;

}

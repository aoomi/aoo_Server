package server.aoo.dao.config;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
public class MongoDbGameProperties {
    /**
     * 连接地址列表
     */
    private List<String> address;
    /**
     * 副本集
     */
    private String replicaSet;
    /**
     * 数据库
     */
    private String database;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;

}
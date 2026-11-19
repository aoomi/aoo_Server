package server.aoo.dao.entity.mongo;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@Document(collection = "DbTagDBDataKey")
@CompoundIndexes({@CompoundIndex(name = "accountID",def = "{'AccountID':1}")})
public class DbTagDBDataKey {
    /**
     *
     */
    @Id
    private int id;

    /**
     * 账号id
     */
    private Long AccountID;

}

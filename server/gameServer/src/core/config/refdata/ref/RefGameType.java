package core.config.refdata.ref;

import com.ddm.server.common.data.RefContainer;
import com.ddm.server.common.data.RefField;
import lombok.Data;

@Data
public class RefGameType extends RefBaseGame {
    @RefField(iskey = true)
    public int Id;
    public String Name;
    public int Type;

    @Override
    public boolean Assert() {
        return true;
    }

    @Override
    public boolean AssertAll(RefContainer<?> all) {
        return true;
    }

    @Override
    public long getId() {
        return Id;
    }
}

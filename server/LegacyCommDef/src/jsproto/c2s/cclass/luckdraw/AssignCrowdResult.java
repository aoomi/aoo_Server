package jsproto.c2s.cclass.luckdraw;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class AssignCrowdResult implements Serializable {
    private boolean isExist;

    public AssignCrowdResult(boolean isExist) {
        this.isExist = isExist;
    }
}

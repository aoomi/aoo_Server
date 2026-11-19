package com.aoo.bcg.gamespi;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
class CommittedCommandBindingTest {
    static CommittedCommandBinding binding(){return new CommittedCommandBinding(2,"user","move",7,0,"req","instance-A",1,2,3);}
    @Test void scopeIsExact(){var b=binding(); b.requireScope("user","move",7,0,"req"); assertThrows(IllegalArgumentException.class,()->b.requireScope("other","move",7,0,"req"));}
    @Test void rejectsInvalidIdentity(){assertThrows(IllegalArgumentException.class,()->new CommittedCommandBinding(2,"user","move",7,0,"req","A",0,2,3));}
    @Test void legacyIsUnknown(){assertNull(new GameCommandResult("move","req",Map.of("roomInstanceId","spoof")).committedBinding());}
    @Test void helpersPreserveAndRejectReplacement(){var b=binding();var value=new GameCommandResult("move","req",Map.of()).withCommittedBinding(b);assertEquals(b,value.withAuthorityMetadata(3,4).committedBinding());assertThrows(IllegalArgumentException.class,()->value.withCommittedBinding(new CommittedCommandBinding(2,"user","move",7,0,"req","B",2,3,4)));}
}

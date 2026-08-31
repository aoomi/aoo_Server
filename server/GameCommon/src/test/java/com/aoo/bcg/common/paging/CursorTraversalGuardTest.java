package com.aoo.bcg.common.paging;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class CursorTraversalGuardTest{@Test void rejectsEmptyRepeatedAndEndlessPages(){var g=new CursorTraversalGuard(2);g.accept(null,"10",10,true);assertThrows(IllegalStateException.class,()->g.accept("10","10",10,true));var e=new CursorTraversalGuard(2);assertThrows(IllegalStateException.class,()->e.accept(null,"1",0,true));var b=new CursorTraversalGuard(1);b.accept(null,null,0,false);assertThrows(IllegalStateException.class,()->b.accept(null,null,0,false));}}

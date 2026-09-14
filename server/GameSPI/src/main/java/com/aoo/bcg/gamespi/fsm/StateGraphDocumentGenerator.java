package com.aoo.bcg.gamespi.fsm;
import java.nio.file.Files;import java.nio.file.Path;
public final class StateGraphDocumentGenerator{private StateGraphDocumentGenerator(){}public static void main(String[]args)throws Exception{if(args.length!=1)throw new IllegalArgumentException("output path required");Path output=Path.of(args[0]);Files.createDirectories(output.getParent());Files.writeString(output,new StateGraphRenderer().mermaid("Aoo 通用房间状态机",CommonRoomLifecycle.transitions()));}}

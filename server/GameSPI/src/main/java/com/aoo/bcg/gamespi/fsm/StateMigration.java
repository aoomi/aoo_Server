package com.aoo.bcg.gamespi.fsm;
import java.util.Map;
public interface StateMigration{String fromPlayVersion();String toPlayVersion();boolean supportsInProgressRound();Map<String,Object> convert(Map<String,Object> sourceState);}

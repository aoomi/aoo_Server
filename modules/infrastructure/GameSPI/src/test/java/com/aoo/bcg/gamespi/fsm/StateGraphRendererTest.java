package com.aoo.bcg.gamespi.fsm;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class StateGraphRendererTest{@Test void graphIsGeneratedFromDeclaredTransitions(){String graph=new StateGraphRenderer().mermaid("room",CommonRoomLifecycle.transitions());assertTrue(graph.contains("stateDiagram-v2"));assertTrue(graph.contains("WAITING --> PLAYING : START [allReady] / startRound"));assertEquals(CommonRoomLifecycle.transitions().size(),graph.lines().filter(line->line.contains(" --> ")).count());}}

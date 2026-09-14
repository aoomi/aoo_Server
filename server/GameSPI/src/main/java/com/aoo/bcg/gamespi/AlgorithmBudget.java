package com.aoo.bcg.gamespi;
import java.time.Duration;import java.util.Objects;
/** Per-invocation guard for card combinations, rule trees and hint decomposition. */
public final class AlgorithmBudget{
 private final int maximumSteps,maximumDepth;private final long deadlineNanos;private int steps,depth;
 private AlgorithmBudget(int inputSize,int maximumInput,int maximumSteps,int maximumDepth,Duration maximumElapsed){if(inputSize<0||inputSize>maximumInput||maximumInput<1||maximumSteps<1||maximumDepth<1||maximumElapsed.isNegative()||maximumElapsed.isZero())throw new IllegalArgumentException("invalid algorithm budget/input");this.maximumSteps=maximumSteps;this.maximumDepth=maximumDepth;this.deadlineNanos=Math.addExact(System.nanoTime(),maximumElapsed.toNanos());}
 public static AlgorithmBudget start(int inputSize,int maximumInput,int maximumSteps,int maximumDepth,Duration maximumElapsed){return new AlgorithmBudget(inputSize,maximumInput,maximumSteps,maximumDepth,Objects.requireNonNull(maximumElapsed));}
 public void step(){if(++steps>maximumSteps||System.nanoTime()>deadlineNanos)throw new BudgetExceededException("algorithm step/time budget exceeded");}
 public Depth enter(){step();if(++depth>maximumDepth){depth--;throw new BudgetExceededException("algorithm depth budget exceeded");}return new Depth();}
 public int steps(){return steps;}
 public final class Depth implements AutoCloseable{private boolean closed;private Depth(){}public void close(){if(!closed){closed=true;depth--;}}}
 public static final class BudgetExceededException extends RuntimeException{public BudgetExceededException(String message){super(message);}}
}

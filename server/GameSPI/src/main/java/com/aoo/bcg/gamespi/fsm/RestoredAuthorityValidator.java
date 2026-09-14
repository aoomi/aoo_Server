package com.aoo.bcg.gamespi.fsm;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;
public final class RestoredAuthorityValidator{private RestoredAuthorityValidator(){}public static <T extends AuthoritativeGameSession>T requireConsistent(T session){var violations=session.invariantViolations();if(!violations.isEmpty())throw new IllegalStateException("invalid restored authority: "+String.join(",",violations));return session;}}

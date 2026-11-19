package com.ddm.server.common.semantic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares intentional reference identity for a short-lived mutable aggregate or algorithm key. */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface IdentitySemantics { }

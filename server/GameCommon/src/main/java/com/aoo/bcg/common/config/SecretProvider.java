package com.aoo.bcg.common.config;

@FunctionalInterface
public interface SecretProvider {
    SecretMaterial resolve(SecretReference reference);
}

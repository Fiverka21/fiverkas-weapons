package com.fiv.fiverkas_weapons.config;

public final class ModClientConfig {
    public static final ConfigValue<String> DSHIELD_RESILIENCE_KEY =
            new ConfigValue<>("key.mouse.middle");

    private ModClientConfig() {
    }

    public record ConfigValue<T>(T value) {
        public T get() {
            return value;
        }
    }
}

package com.fiv.fiverkas_weapons.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> DSHIELD_RESILIENCE_KEY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        DSHIELD_RESILIENCE_KEY = builder
                .comment("Default key for activating charged dshield resilience. Examples: key.mouse.middle, key.keyboard.r")
                .define("dshieldResilienceKey", "key.mouse.middle");
        SPEC = builder.build();
    }

    private ModClientConfig() {
    }
}

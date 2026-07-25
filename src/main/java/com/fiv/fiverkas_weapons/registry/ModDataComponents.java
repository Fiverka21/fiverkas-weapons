package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModDataComponents {
    public static final DataComponentType<Integer> DSHIELD_EYES =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    FiverkasWeapons.id("dshield_eyes"),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 6))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build()
            );

    public static void init() {
    }

    private ModDataComponents() {
    }
}

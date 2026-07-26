package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FiverkasWeapons.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DSHIELD_EYES =
            DATA_COMPONENT_TYPES.registerComponentType(
                    "dshield_eyes",
                    builder -> builder
                            .persistent(Codec.intRange(0, 6))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
            );

    private ModDataComponents() {
    }
}

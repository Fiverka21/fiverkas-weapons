package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.effect.BleedEffect;
import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.effect.CeruleanShroudEffect;
import com.fiv.fiverkas_weapons.effect.SunsetEffect;
import com.fiv.fiverkas_weapons.effect.VaporifiedEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, FiverkasWeapons.MODID);

    public static final DeferredHolder<MobEffect, VaporifiedEffect> VAPORIFIED =
            EFFECTS.register("vaporified", VaporifiedEffect::new);

    public static final DeferredHolder<MobEffect, BleedEffect> BLEED =
            EFFECTS.register("bleed", BleedEffect::new);

    public static final DeferredHolder<MobEffect, CeruleanShroudEffect> CERULEAN_SHROUD =
            EFFECTS.register("cerulean_shroud", CeruleanShroudEffect::new);

    public static final DeferredHolder<MobEffect, SunsetEffect> SUNSET =
            EFFECTS.register("sunset", SunsetEffect::new);

    public static Holder<MobEffect> vaporifiedHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(VAPORIFIED.get());
    }

    public static Holder<MobEffect> bleedHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(BLEED.get());
    }

    public static Holder<MobEffect> ceruleanShroudHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(CERULEAN_SHROUD.get());
    }

    public static Holder<MobEffect> sunsetHolder() {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(SUNSET.get());
    }
}

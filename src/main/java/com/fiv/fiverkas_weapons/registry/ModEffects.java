package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.effect.BleedEffect;
import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.effect.CeruleanShroudEffect;
import com.fiv.fiverkas_weapons.effect.ResilienceEffect;
import com.fiv.fiverkas_weapons.effect.SunsetEffect;
import com.fiv.fiverkas_weapons.effect.VaporifiedEffect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

public class ModEffects {

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, FiverkasWeapons.id(name), effect);
    }

    public static final Holder<MobEffect> VAPORIFIED =
            register("vaporified", new VaporifiedEffect());

    public static final Holder<MobEffect> BLEED =
            register("bleed", new BleedEffect());

    public static final Holder<MobEffect> CERULEAN_SHROUD =
            register("cerulean_shroud", new CeruleanShroudEffect());

    public static final Holder<MobEffect> SUNSET =
            register("sunset", new SunsetEffect());

    public static final Holder<MobEffect> RESILIENCE =
            register("resilience", new ResilienceEffect());

    public static void init() {
    }
}

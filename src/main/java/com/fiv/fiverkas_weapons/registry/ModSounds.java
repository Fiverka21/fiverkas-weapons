package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public class ModSounds {
    private static Supplier<SoundEvent> register(String name) {
        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(FiverkasWeapons.id(name));
        SoundEvent registered = Registry.register(BuiltInRegistries.SOUND_EVENT, FiverkasWeapons.id(name), soundEvent);
        return () -> registered;
    }

    public static final Supplier<SoundEvent> MKOPI = register("mkopi");

    public static final Supplier<SoundEvent> DUSK = register("dusk");

    public static final Supplier<SoundEvent> RAMIEL = register("ramiel");

    public static final Supplier<SoundEvent> TP = register("tp");

    public static void init() {
    }
}

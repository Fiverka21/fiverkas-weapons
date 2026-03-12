package com.fiv.fiverkas_weapons.registry;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.util.CompatIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, FiverkasWeapons.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MKOPI =
            SOUND_EVENTS.register("mkopi",
                    () -> CompatIds.soundEvent(FiverkasWeapons.MODID, "mkopi"));
}

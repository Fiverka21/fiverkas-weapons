package com.fiv.fiverkas_weapons.fabric.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class PersistentData {
    private PersistentData() {
    }

    public static CompoundTag get(Entity entity) {
        return ((PersistentDataAccessor) entity).fweapons$getPersistentData();
    }
}

package com.fiv.fiverkas_weapons.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.extensions.IEntityExtension;

public final class EntityDataUtil {
    private EntityDataUtil() {
    }

    public static CompoundTag getPersistentData(Entity entity) {
        return ((IEntityExtension) entity).getPersistentData();
    }

    public static double getDouble(CompoundTag data, String key) {
        return data.getDouble(key).orElse(0.0D);
    }

    public static int getInt(CompoundTag data, String key) {
        return data.getInt(key).orElse(0);
    }

    public static long getLong(CompoundTag data, String key) {
        return data.getLong(key).orElse(0L);
    }

    public static boolean getBoolean(CompoundTag data, String key) {
        return data.getBoolean(key).orElse(false);
    }
}

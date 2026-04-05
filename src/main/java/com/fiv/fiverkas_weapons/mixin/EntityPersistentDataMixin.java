package com.fiv.fiverkas_weapons.mixin;

import com.fiv.fiverkas_weapons.fabric.data.PersistentDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements PersistentDataAccessor {
    private static final String FWEAPONS_PERSISTENT_DATA_KEY = "fweapons_persistent_data";

    @Unique
    private CompoundTag fweapons$persistentData;

    @Override
    public CompoundTag fweapons$getPersistentData() {
        if (fweapons$persistentData == null) {
            fweapons$persistentData = new CompoundTag();
        }
        return fweapons$persistentData;
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void fweapons$writePersistentData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (fweapons$persistentData != null && !fweapons$persistentData.isEmpty()) {
            tag.put(FWEAPONS_PERSISTENT_DATA_KEY, fweapons$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void fweapons$readPersistentData(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(FWEAPONS_PERSISTENT_DATA_KEY, 10)) {
            fweapons$persistentData = tag.getCompound(FWEAPONS_PERSISTENT_DATA_KEY).copy();
        }
    }
}

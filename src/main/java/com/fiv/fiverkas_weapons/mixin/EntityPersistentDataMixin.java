package com.fiv.fiverkas_weapons.mixin;

import com.fiv.fiverkas_weapons.fabric.data.PersistentDataAccessor;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPersistentDataMixin implements PersistentDataAccessor, IEntityExtension {
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

    @Override
    public CompoundTag getPersistentData() {
        return fweapons$getPersistentData();
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"))
    private void fweapons$writePersistentData(ValueOutput output, CallbackInfo ci) {
        if (fweapons$persistentData != null && !fweapons$persistentData.isEmpty()) {
            output.store(FWEAPONS_PERSISTENT_DATA_KEY, CompoundTag.CODEC, fweapons$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void fweapons$readPersistentData(ValueInput input, CallbackInfo ci) {
        fweapons$persistentData = input.read(FWEAPONS_PERSISTENT_DATA_KEY, CompoundTag.CODEC)
                .map(CompoundTag::copy)
                .orElse(null);
    }
}

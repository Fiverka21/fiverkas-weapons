package com.fiv.fiverkas_weapons.mixin;

import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds Harvester's second loot-table roll after vanilla drops have been created. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHarvesterLootMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void fweapons$addHarvesterBonusLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        ModCombatEvents.addHarvesterBonusLoot(level, (LivingEntity) (Object) this, source);
    }
}

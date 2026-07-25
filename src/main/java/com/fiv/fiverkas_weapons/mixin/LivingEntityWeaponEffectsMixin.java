package com.fiv.fiverkas_weapons.mixin;

import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies weapon-on-hit effects after vanilla confirms that damage was dealt. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityWeaponEffectsMixin {
    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void fweapons$applyWeaponEffects(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ()) {
            ModCombatEvents.onLivingDamageApplied((LivingEntity) (Object) this, source);
        }
    }
}

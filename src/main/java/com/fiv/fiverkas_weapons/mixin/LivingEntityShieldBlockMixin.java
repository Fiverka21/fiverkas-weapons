package com.fiv.fiverkas_weapons.mixin;

import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityShieldBlockMixin {
    @Inject(
            method = "hurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"
            )
    )
    private void fweapons$onShieldBlock(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity blocker = (LivingEntity) (Object) this;
        ModCombatEvents.onLivingShieldBlock(
                new ModCombatEvents.LivingShieldBlockEvent(blocker, true, amount)
        );
    }
}

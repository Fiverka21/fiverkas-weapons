package com.fiv.fiverkas_weapons.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Shadow
    protected abstract void actuallyHurt(DamageSource source, float amount);

    @Unique
    private boolean fweapons$rewrappingHurt;
    @Unique
    private boolean fweapons$rewrappingActuallyHurt;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void fweapons$onHurtHead(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide || fweapons$rewrappingHurt) {
            return;
        }

        LivingIncomingDamageEvent event = new LivingIncomingDamageEvent(self, source, amount);
        NeoForge.EVENT_BUS.post(event);
        float adjusted = event.getAmount();
        if (adjusted <= 0.0F) {
            cir.setReturnValue(false);
            return;
        }
        if (Math.abs(adjusted - amount) <= 1.0E-4F) {
            return;
        }

        fweapons$rewrappingHurt = true;
        try {
            boolean result = self.hurt(source, adjusted);
            cir.setReturnValue(result);
        } finally {
            fweapons$rewrappingHurt = false;
        }
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void fweapons$onActuallyHurtPre(
            DamageSource source,
            float amount,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide || fweapons$rewrappingActuallyHurt) {
            return;
        }

        LivingDamageEvent.Pre event = new LivingDamageEvent.Pre(self, source, amount, new DamageContainer());
        NeoForge.EVENT_BUS.post(event);
        float adjusted = event.getNewDamage();
        if (adjusted <= 0.0F) {
            ci.cancel();
            return;
        }
        if (Math.abs(adjusted - amount) <= 1.0E-4F) {
            return;
        }

        fweapons$rewrappingActuallyHurt = true;
        try {
            this.actuallyHurt(source, adjusted);
        } finally {
            fweapons$rewrappingActuallyHurt = false;
        }
        ci.cancel();
    }

    @Inject(method = "actuallyHurt", at = @At("TAIL"))
    private void fweapons$onActuallyHurtPost(
            DamageSource source,
            float amount,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide || amount <= 0.0F) {
            return;
        }
        NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(self, source, amount));
    }
}

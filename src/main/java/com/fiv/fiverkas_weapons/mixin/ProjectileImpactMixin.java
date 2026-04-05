package com.fiv.fiverkas_weapons.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileImpactMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void fweapons$onProjectileImpact(HitResult hitResult, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(self, hitResult));
    }
}

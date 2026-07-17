package com.fiv.fiverkas_weapons.mixin;

import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileImpactMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void fweapons$onProjectileImpact(HitResult hitResult, CallbackInfo ci) {
        ModCombatEvents.onProjectileImpact(
                new ModCombatEvents.ProjectileImpactEvent((Projectile) (Object) this, hitResult)
        );
    }
}

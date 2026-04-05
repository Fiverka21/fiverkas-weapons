package com.fiv.fiverkas_weapons.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Mob.class)
public abstract class MobSetTargetMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity fweapons$onSetTarget(LivingEntity target) {
        Mob self = (Mob) (Object) this;
        LivingChangeTargetEvent event = new LivingChangeTargetEvent(self, target);
        NeoForge.EVENT_BUS.post(event);
        return event.getNewAboutToBeSetTarget();
    }
}

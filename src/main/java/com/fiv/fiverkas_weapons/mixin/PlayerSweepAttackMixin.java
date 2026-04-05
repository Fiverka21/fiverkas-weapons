package com.fiv.fiverkas_weapons.mixin;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerSweepAttackMixin {
    @Shadow
    protected abstract void sweepAttack();

    @Redirect(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;sweepAttack()V"
            )
    )
    private void fweapons$onSweepAttack(Player player) {
        SweepAttackEvent event = new SweepAttackEvent(player, true);
        NeoForge.EVENT_BUS.post(event);
        if (!event.isCanceled() && event.isSweeping()) {
            this.sweepAttack();
        }
    }
}

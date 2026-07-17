package com.fiv.fiverkas_weapons.mixin;

import com.fiv.fiverkas_weapons.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerDShieldDurabilityMixin {
    @Redirect(
            method = "hurtCurrentlyUsedShield",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"
            )
    )
    private boolean fweapons$isVanillaShieldOrDShield(ItemStack stack, Item item) {
        return stack.is(item) || stack.is(ModItems.DSHIELD.get());
    }
}

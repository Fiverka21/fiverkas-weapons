package com.fiv.fiverkas_weapons.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Shadow
    @Final
    private DataSlot cost;

    @Shadow
    private String itemName;

    @Shadow
    private int repairItemCountCost;

    @Inject(method = "createResult", at = @At("TAIL"))
    private void fweapons$onCreateResult(CallbackInfo ci) {
        ItemCombinerMenuAccessor access = (ItemCombinerMenuAccessor) this;
        Container inputSlots = access.fweapons$getInputSlots();
        ItemStack left = inputSlots.getItem(0);
        if (left.isEmpty()) {
            return;
        }
        AnvilUpdateEvent event = new AnvilUpdateEvent(
                access.fweapons$getPlayer(),
                left,
                inputSlots.getItem(1),
                itemName
        );
        NeoForge.EVENT_BUS.post(event);
        if (event.getOutput().isEmpty()) {
            return;
        }
        access.fweapons$getResultSlots().setItem(0, event.getOutput());
        cost.set((int) event.getCost());
        repairItemCountCost = event.getMaterialCost();
    }
}

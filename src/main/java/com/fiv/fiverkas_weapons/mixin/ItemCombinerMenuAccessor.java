package com.fiv.fiverkas_weapons.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
    @Accessor("inputSlots")
    Container fweapons$getInputSlots();

    @Accessor("resultSlots")
    ResultContainer fweapons$getResultSlots();

    @Accessor("player")
    Player fweapons$getPlayer();
}

package com.fiv.fiverkas_weapons.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;

public class GBlueprintItem extends AnimatedGradientItem {
    public GBlueprintItem(Properties properties, int startColor, int endColor, long colorShiftSpeedMs) {
        super(properties, startColor, endColor, colorShiftSpeedMs);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.fweapons.gblueprint").withStyle(ChatFormatting.ITALIC));
    }
}

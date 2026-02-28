package com.fiv.fiverkas_weapons.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class GBlueprintItem extends AnimatedGradientItem {
    public GBlueprintItem(Properties properties, int startColor, int endColor, long colorShiftSpeedMs) {
        super(properties, startColor, endColor, colorShiftSpeedMs);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fweapons.gblueprint").withStyle(ChatFormatting.ITALIC));
    }
}

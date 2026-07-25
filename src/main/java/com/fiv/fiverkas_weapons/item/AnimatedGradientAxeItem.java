package com.fiv.fiverkas_weapons.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import org.jetbrains.annotations.NotNull;

public class AnimatedGradientAxeItem extends AxeItem {
    private final AnimatedGradientNameCache nameCache;

    protected AnimatedGradientAxeItem(
            ToolMaterial tier,
            Item.Properties properties,
            int startColor,
            int endColor,
            long colorShiftSpeedMs
    ) {
        this(tier, properties, startColor, endColor, colorShiftSpeedMs, WeaponNameFonts.DEFAULT);
    }

    protected AnimatedGradientAxeItem(
            ToolMaterial tier,
            Item.Properties properties,
            int startColor,
            int endColor,
            long colorShiftSpeedMs,
            Identifier nameFont
    ) {
        super(tier, 0.0F, 0.0F, properties);
        this.nameCache = new AnimatedGradientNameCache(startColor, endColor, colorShiftSpeedMs, nameFont);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return nameCache.getName(getDescriptionId(), super.getName(stack));
    }
}

package com.fiv.fiverkas_weapons.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AnimatedGradientItem extends Item {
    private final AnimatedGradientNameCache nameCache;

    public AnimatedGradientItem(
            Item.Properties properties,
            int startColor,
            int endColor,
            long colorShiftSpeedMs
    ) {
        this(properties, startColor, endColor, colorShiftSpeedMs, WeaponNameFonts.DEFAULT);
    }

    public AnimatedGradientItem(
            Item.Properties properties,
            int startColor,
            int endColor,
            long colorShiftSpeedMs,
            ResourceLocation nameFont
    ) {
        super(properties);
        this.nameCache = new AnimatedGradientNameCache(startColor, endColor, colorShiftSpeedMs, nameFont);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return nameCache.getName(getDescriptionId(stack), super.getName(stack));
    }
}

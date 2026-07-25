package com.fiv.fiverkas_weapons.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import org.jetbrains.annotations.NotNull;

public class AnimatedGradientSwordItem extends Item {
    private final AnimatedGradientNameCache nameCache;

    protected AnimatedGradientSwordItem(
            ToolMaterial tier,
            Item.Properties properties,
            int startColor,
            int endColor,
            long colorShiftSpeedMs
    ) {
        this(tier, properties, startColor, endColor, colorShiftSpeedMs, WeaponNameFonts.DEFAULT);
    }

    protected AnimatedGradientSwordItem(
            ToolMaterial tier,
            Item.Properties properties,
            int startColor,
            int endColor,
            long colorShiftSpeedMs,
            Identifier nameFont
    ) {
        super(properties);
        this.nameCache = new AnimatedGradientNameCache(startColor, endColor, colorShiftSpeedMs, nameFont);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return nameCache.getName(getDescriptionId(), super.getName(stack));
    }
}

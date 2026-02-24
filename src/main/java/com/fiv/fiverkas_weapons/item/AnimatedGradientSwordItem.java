package com.fiv.fiverkas_weapons.item;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public class AnimatedGradientSwordItem extends SwordItem {
    private final int startColor;
    private final int endColor;
    private final long colorShiftSpeedMs;

    protected AnimatedGradientSwordItem(
            Tier tier,
            Item.Properties properties,
            int startColor,
            int endColor,
            long colorShiftSpeedMs
    ) {
        super(tier, properties);
        this.startColor = startColor;
        this.endColor = endColor;
        this.colorShiftSpeedMs = colorShiftSpeedMs;
    }

    @Override
    public Component getName(ItemStack stack) {
        String baseName = Component.translatable(this.getDescriptionId(stack)).getString();
        if (baseName.isEmpty()) {
            return super.getName(stack);
        }

        MutableComponent gradientName = Component.empty();
        float timeOffset = Util.getMillis() / (float) colorShiftSpeedMs;
        int[] codePoints = baseName.codePoints().toArray();

        for (int i = 0; i < codePoints.length; i++) {
            float wave = (float) ((Math.sin((i + timeOffset) * 0.55f) + 1.0d) * 0.5d);
            int color = blend(startColor, endColor, wave);
            String glyph = new String(Character.toChars(codePoints[i]));

            gradientName.append(
                    Component.literal(glyph).withColor(color)
            );
        }

        return gradientName;
    }

    private static int blend(int startColor, int endColor, float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));

        int startR = (startColor >> 16) & 0xFF;
        int startG = (startColor >> 8) & 0xFF;
        int startB = startColor & 0xFF;

        int endR = (endColor >> 16) & 0xFF;
        int endG = (endColor >> 8) & 0xFF;
        int endB = endColor & 0xFF;

        int r = (int) (startR + (endR - startR) * clamped);
        int g = (int) (startG + (endG - startG) * clamped);
        int b = (int) (startB + (endB - startB) * clamped);

        return (r << 16) | (g << 8) | b;
    }
}

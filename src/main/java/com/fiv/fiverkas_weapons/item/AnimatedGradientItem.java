package com.fiv.fiverkas_weapons.item;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AnimatedGradientItem extends Item {
    private static final long NAME_CACHE_REFRESH_MS = 1000L;
    private static final long GRADIENT_FRAME_MS = 25L;

    private final int startColor;
    private final int endColor;
    private final long colorShiftSpeedMs;

    private long nextNameRefreshMs;
    private String cachedBaseName = "";
    private String[] cachedGlyphs = new String[0];

    private long cachedGradientFrame = Long.MIN_VALUE;
    private Component cachedGradientName = Component.empty();

    public AnimatedGradientItem(
            Item.Properties properties,
            int startColor,
            int endColor,
            long colorShiftSpeedMs
    ) {
        super(properties);
        this.startColor = startColor;
        this.endColor = endColor;
        this.colorShiftSpeedMs = Math.max(1L, colorShiftSpeedMs);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        long nowMs = Util.getMillis();

        if (nowMs >= nextNameRefreshMs || cachedGlyphs.length == 0) {
            nextNameRefreshMs = nowMs + NAME_CACHE_REFRESH_MS;
            refreshNameCache(stack);
        }

        if (cachedGlyphs.length == 0) {
            return super.getName(stack);
        }

        long frame = nowMs / GRADIENT_FRAME_MS;
        if (frame != cachedGradientFrame) {
            cachedGradientFrame = frame;
            float timeOffset = (frame * GRADIENT_FRAME_MS) / (float) colorShiftSpeedMs;
            MutableComponent gradientName = Component.empty();

            for (int i = 0; i < cachedGlyphs.length; i++) {
                float wave = (float) ((Math.sin((i + timeOffset) * 0.55f) + 1.0d) * 0.5d);
                int color = blend(startColor, endColor, wave);

                gradientName.append(
                        Component.literal(cachedGlyphs[i]).withColor(color)
                );
            }

            cachedGradientName = gradientName;
        }

        return cachedGradientName;
    }

    private void refreshNameCache(ItemStack stack) {
        String baseName = Component.translatable(this.getDescriptionId(stack)).getString();

        if (baseName.isEmpty()) {
            if (!cachedBaseName.isEmpty()) {
                cachedBaseName = "";
                cachedGlyphs = new String[0];
                cachedGradientFrame = Long.MIN_VALUE;
                cachedGradientName = Component.empty();
            }
            return;
        }

        if (baseName.equals(cachedBaseName)) {
            return;
        }

        cachedBaseName = baseName;
        int[] codePoints = baseName.codePoints().toArray();
        String[] glyphs = new String[codePoints.length];

        for (int i = 0; i < codePoints.length; i++) {
            glyphs[i] = new String(Character.toChars(codePoints[i]));
        }

        cachedGlyphs = glyphs;
        cachedGradientFrame = Long.MIN_VALUE;
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

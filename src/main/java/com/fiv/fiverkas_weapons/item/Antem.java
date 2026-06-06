package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

public class Antem extends AnimatedGradientSwordItem {
    private static final int WHITE = 0xFFFFFF;
    private static final int ORANGE = 0xFF5A00;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public Antem(Tier tier, Item.Properties properties) {
        super(tier, properties, WHITE, ORANGE, COLOR_SHIFT_SPEED_MS);
    }
}

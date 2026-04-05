package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;

public class NatureAxe extends AnimatedGradientAxeItem {
    private static final int FOREST_GREEN = 0x2F7D32;
    private static final int BARK_BROWN = 0x6D4C41;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public NatureAxe(Item.Properties properties) {
        super(properties, BARK_BROWN, FOREST_GREEN, COLOR_SHIFT_SPEED_MS);
    }
}

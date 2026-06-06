package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;

public class Sacrilegious extends AnimatedGradientSwordItem {
    private static final int BLUE = 0x332EBF;
    private static final int YELLOW = 0xBABF2E;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public Sacrilegious(Tier tier, Item.Properties properties) {
        super(tier, properties, BLUE, YELLOW, COLOR_SHIFT_SPEED_MS);
    }
}

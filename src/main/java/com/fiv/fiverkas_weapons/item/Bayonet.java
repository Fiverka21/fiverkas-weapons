package com.fiv.fiverkas_weapons.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class Bayonet extends AnimatedGradientSwordItem {
    private static final int WHITE = 0xFFFFFF;
    private static final int BLACK = 0X000000;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;

    public Bayonet(Tier tier, Item.Properties properties) {
        super(tier, properties, WHITE, BLACK, COLOR_SHIFT_SPEED_MS);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (itemAbility == ItemAbilities.SWORD_SWEEP) {
            return false;
        }
        return super.canPerformAction(stack, itemAbility);
    }

}

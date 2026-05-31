package com.fiv.fiverkas_weapons.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.Level;

public class DShieldItem extends ShieldItem {
    public DShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel && !player.hasInfiniteMaterials()) {
            ItemStack stack = result.getObject();
            stack.hurtAndBreak(1, serverLevel, player, item -> player.onEquippedItemBroken(item, Player.getSlotForHand(hand)));
        }
        return result;
    }
}

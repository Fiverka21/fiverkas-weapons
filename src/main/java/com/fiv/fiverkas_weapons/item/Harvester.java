package com.fiv.fiverkas_weapons.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Harvester extends AnimatedGradientSwordItem {
    private static final int RED = 0xFF0000;
    private static final int DARK_BLOOD_RED = 0x3A0000;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;
    private static final int CROP_HARVEST_DURABILITY_COST = 2;
    private static final int CROP_HARVEST_DROP_MULTIPLIER = 2;

    public Harvester(ToolMaterial tier, Item.Properties properties) {
        super(tier, properties, RED, DARK_BLOOD_RED, COLOR_SHIFT_SPEED_MS);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        BlockState harvestedState = getHarvestedCropState(state);
        if (harvestedState == null) {
            return super.useOn(context);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        ItemStack tool = context.getItemInHand();
        for (int i = 0; i < CROP_HARVEST_DROP_MULTIPLIER; i++) {
            Block.getDrops(state, serverLevel, pos, null, player, tool)
                    .forEach(drop -> Block.popResource(serverLevel, pos, drop));
        }

        level.setBlock(pos, harvestedState, 2);
        if (player != null) {
            tool.hurtAndBreak(CROP_HARVEST_DURABILITY_COST, player, getEquipmentSlot(context.getHand()));
        }
        return InteractionResult.CONSUME;
    }

    private static BlockState getHarvestedCropState(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
            return cropBlock.getStateForAge(0);
        }
        if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
            return state.setValue(NetherWartBlock.AGE, 0);
        }
        return null;
    }

    private static EquipmentSlot getEquipmentSlot(InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }
}

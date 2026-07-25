package com.fiv.fiverkas_weapons.item;

import com.fiv.fiverkas_weapons.registry.ModDataComponents;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import com.fiv.fiverkas_weapons.registry.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class DShieldItem extends ShieldItem {
    public static final int CHARGES_REQUIRED = 8;
    public static final int EYE_CAPACITY = 6;
    public static final int RESILIENCE_DURATION_TICKS = 200;

    public DShieldItem(Properties properties) {
        super(properties);
    }

    public static int getChargeCount(ItemStack stack) {
        Float value = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY).getFloat(0);
        return value == null ? 0 : value.intValue();
    }

    public static void addBlockCharge(ItemStack stack) {
        int charges = Math.min(getChargeCount(stack) + 1, CHARGES_REQUIRED);
        setChargeCount(stack, charges);
    }

    public static void setChargeCount(ItemStack stack, int charges) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) charges), List.of(), List.of(), List.of()));
    }

    public static int getStoredEyes(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DSHIELD_EYES, 0);
    }

    private static void setStoredEyes(ItemStack stack, int eyes) {
        stack.set(ModDataComponents.DSHIELD_EYES, Math.clamp(eyes, 0, EYE_CAPACITY));
    }

    public static boolean activateResilience(ServerPlayer player) {
        ItemStack shieldStack = getChargedHeldShield(player);
        if (shieldStack.isEmpty()) {
            return false;
        }
        setChargeCount(shieldStack, 0);
        boolean consumedEye = consumeStoredEye(shieldStack);
        int amplifier = consumedEye ? 1 : 0;
        player.addEffect(new MobEffectInstance(ModEffects.RESILIENCE, RESILIENCE_DURATION_TICKS, amplifier), player);
        return true;
    }

    private static ItemStack getChargedHeldShield(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(ModItems.DSHIELD.get()) && getChargeCount(offhand) >= CHARGES_REQUIRED) {
            return offhand;
        }
        ItemStack mainhand = player.getMainHandItem();
        if (mainhand.is(ModItems.DSHIELD.get()) && getChargeCount(mainhand) >= CHARGES_REQUIRED) {
            return mainhand;
        }
        return ItemStack.EMPTY;
    }

    private static boolean consumeStoredEye(ItemStack stack) {
        int eyes = getStoredEyes(stack);
        if (eyes <= 0) {
            return false;
        }
        setStoredEyes(stack, eyes - 1);
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack shieldStack,
            ItemStack other,
            Slot slot,
            ClickAction action,
            Player player,
            SlotAccess access
    ) {
        if (action != ClickAction.PRIMARY || !slot.allowModification(player)) {
            return false;
        }
        if (!other.is(Items.ENDER_EYE) || getStoredEyes(shieldStack) >= EYE_CAPACITY) {
            return false;
        }
        setStoredEyes(shieldStack, getStoredEyes(shieldStack) + 1);
        if (!player.hasInfiniteMaterials()) {
            other.shrink(1);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        tooltipComponents.accept(
                Component.translatable("tooltip.fweapons.dshield.eyes", getStoredEyes(stack), EYE_CAPACITY)
                        .withStyle(ChatFormatting.GRAY)
        );
    }
}

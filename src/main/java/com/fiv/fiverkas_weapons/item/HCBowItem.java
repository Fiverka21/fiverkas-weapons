package com.fiv.fiverkas_weapons.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;

public class HCBowItem extends CrossbowItem {
    public static final float DAMAGE_MULTIPLIER = 1.4F;
    public static final float CHARGE_DURATION_MULTIPLIER = 0.7F;
    public static final String PROJECTILE_DAMAGE_TAG = "fweapons_hcbow_damage_bonus";

    public HCBowItem(Properties properties) {
        super(properties);
    }

    public static int getChargeDuration(ItemStack stack, LivingEntity shooter) {
        int vanillaDuration = CrossbowItem.getChargeDuration(stack, shooter);
        return Math.max(1, Mth.floor(vanillaDuration * CHARGE_DURATION_MULTIPLIER));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return getChargeDuration(stack, entity) + 3;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        int usedTicks = this.getUseDuration(stack, entityLiving) - timeLeft;
        float chargeProgress = getPowerForTime(usedTicks, stack, entityLiving);
        if (chargeProgress >= 1.0F && !isCharged(stack) && tryLoadProjectiles(entityLiving, stack)) {
            level.playSound(
                    null,
                    entityLiving.getX(),
                    entityLiving.getY(),
                    entityLiving.getZ(),
                    SoundEvents.CROSSBOW_LOADING_END.value(),
                    entityLiving.getSoundSource(),
                    1.0F,
                    1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F
            );
        }
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, isCrit);
        projectile.getPersistentData().putBoolean(PROJECTILE_DAMAGE_TAG, true);
        return projectile;
    }

    private static boolean tryLoadProjectiles(LivingEntity shooter, ItemStack crossbowStack) {
        List<ItemStack> projectiles = draw(crossbowStack, shooter.getProjectile(crossbowStack), shooter);
        if (projectiles.isEmpty()) {
            return false;
        }
        crossbowStack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(projectiles));
        return true;
    }

    private static float getPowerForTime(int usedTicks, ItemStack stack, LivingEntity shooter) {
        float chargeProgress = (float) usedTicks / (float) getChargeDuration(stack, shooter);
        return chargeProgress > 1.0F ? 1.0F : chargeProgress;
    }
}

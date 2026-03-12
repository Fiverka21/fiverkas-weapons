package com.fiv.fiverkas_weapons.item;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.effect.CeruleanShroudEffect;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.fiv.fiverkas_weapons.util.CompatIds;
import com.fiv.fiverkas_weapons.util.EntityDataUtil;
import org.jetbrains.annotations.NotNull;

public class BlueKatana extends AnimatedGradientSwordItem {
    private static final int BLUE_LIGHT = 0x66CCFF;
    private static final int BLUE_DEEP = 0x1D3CFF;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;
    private static final int CERULEAN_SHROUD_DURATION_TICKS = 80;
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(0xFF0000, 1.6F);
    private static final DustParticleOptions CYAN_DUST = new DustParticleOptions(0x00FFFF, 1.3F);
    private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(0x0000FF, 1.2F);
    private static final ResourceKey<DamageType> HONOR_DAMAGE = CompatIds.resourceKey(
            Registries.DAMAGE_TYPE,
            FiverkasWeapons.MODID,
            "blue_katana_honor"
    );

    public BlueKatana(Item.Properties properties) {
        super(properties, BLUE_LIGHT, BLUE_DEEP, COLOR_SHIFT_SPEED_MS);
    }

    @Override
    public @NotNull InteractionResult use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            DamageSource honorDamage = new DamageSource(
                    level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(HONOR_DAMAGE)
            );
            player.hurt(honorDamage, 2.0F);
            if (player.isAlive()) {
                var data = EntityDataUtil.getPersistentData(player);
                data.putDouble(CeruleanShroudEffect.STEP_PROGRESS_TAG, 0.0D);
                data.putDouble(CeruleanShroudEffect.LAST_X_TAG, player.getX());
                data.putDouble(CeruleanShroudEffect.LAST_Y_TAG, player.getY());
                data.putDouble(CeruleanShroudEffect.LAST_Z_TAG, player.getZ());
                player.addEffect(new MobEffectInstance(ModEffects.CERULEAN_SHROUD, CERULEAN_SHROUD_DURATION_TICKS, 0, false, false, true));
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        RED_DUST,
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ(),
                        60,
                        0.55D,
                        0.75D,
                        0.55D,
                        0.0D
                );
                serverLevel.sendParticles(
                        CYAN_DUST,
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ(),
                        40,
                        0.5D,
                        0.7D,
                        0.5D,
                        0.0D
                );
                serverLevel.sendParticles(
                        BLUE_DUST,
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ(),
                        30,
                        0.45D,
                        0.65D,
                        0.45D,
                        0.0D
                );
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
}

package com.fiv.fiverkas_weapons.item;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.effect.CeruleanShroudEffect;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class BlueKatana extends AnimatedGradientSwordItem {
    private static final int BLUE_LIGHT = 0x66CCFF;
    private static final int BLUE_DEEP = 0x1D3CFF;
    private static final long COLOR_SHIFT_SPEED_MS = 144L;
    private static final int CERULEAN_SHROUD_DURATION_TICKS = 80;
    private static final Vector3f RED = Vec3.fromRGB24(0xFF0000).toVector3f();
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(RED, 1.6F);
    private static final Vector3f CYAN = Vec3.fromRGB24(0x00FFFF).toVector3f();
    private static final DustParticleOptions CYAN_DUST = new DustParticleOptions(CYAN, 1.3F);
    private static final Vector3f BLUE = Vec3.fromRGB24(0x0000FF).toVector3f();
    private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(BLUE, 1.2F);
    private static final ResourceKey<DamageType> HONOR_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "blue_katana_honor")
    );

    public BlueKatana(Tier tier, Item.Properties properties) {
        super(tier, properties, BLUE_LIGHT, BLUE_DEEP, COLOR_SHIFT_SPEED_MS);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            DamageSource honorDamage = new DamageSource(
                    level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HONOR_DAMAGE)
            );
            player.hurt(honorDamage, 2.0F);
            if (player.isAlive()) {
                player.getPersistentData().putDouble(CeruleanShroudEffect.STEP_PROGRESS_TAG, 0.0D);
                player.getPersistentData().putDouble(CeruleanShroudEffect.LAST_X_TAG, player.getX());
                player.getPersistentData().putDouble(CeruleanShroudEffect.LAST_Y_TAG, player.getY());
                player.getPersistentData().putDouble(CeruleanShroudEffect.LAST_Z_TAG, player.getZ());
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
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}

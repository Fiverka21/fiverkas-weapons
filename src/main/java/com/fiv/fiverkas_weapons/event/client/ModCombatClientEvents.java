package com.fiv.fiverkas_weapons.event.client;

import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.network.BayonetMuzzleFlashPayload;
import com.fiv.fiverkas_weapons.network.ClientAttackFlagPayload;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

public final class ModCombatClientEvents {
    private static final String BAYONET_GUNSHOT_ANIMATION = "fweapons:bayonet_no_swing";
    private static final String BAYONET_GUNSHOT_HITBOX = "FORWARD_BOX";
    private static final String MKOPI_SLAM_ANIMATION = "bettercombat:two_handed_slam";
    private static final String MKOPI_SLAM_HITBOX = "VERTICAL_PLANE";
    private static final String TRAIL_PARTICLE_TYPE_NONE = "none";
    private static final int BURST_COUNT = 18;
    private static volatile List<?> noTrailParticles;

    private ModCombatClientEvents() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ModCombatClientEvents::init);
    }

    private static void init() {
        registerClientRenderHooks();
        registerBetterCombatAttackStartListener();
        registerBetterCombatAttackHitListener();
    }

    private static void registerClientRenderHooks() {
        NeoForge.EVENT_BUS.addListener(ModCombatClientEvents::onRenderPlayerPre);
    }

    private static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        AvatarRenderState state = (AvatarRenderState) event.getRenderState();
        Entity entity = level.getEntity(state.id);
        if (entity instanceof LivingEntity living && living.hasEffect(ModEffects.CERULEAN_SHROUD)) {
            event.setCanceled(true);
        }
    }


    private static void registerBetterCombatAttackStartListener() {
        try {
            Class<?> eventsClass = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents");
            Object attackStartPublisher = eventsClass.getField("ATTACK_START").get(null);
            Class<?> listenerClass = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents$PlayerAttackStart");

            Object listener = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    (proxy, method, args) -> {
                        if (!"onPlayerAttackStart".equals(method.getName())
                                || args == null
                                || args.length < 2
                                || !(args[0] instanceof LocalPlayer player)) {
                            return null;
                        }

                        Object attackHand = args[1];
                        if (attackHand != null) {
                            if (isBayonetThirdAttack(attackHand)) {
                                spawnBayonetGunshotParticles(player);
                                sendToServer(new BayonetMuzzleFlashPayload());
                            }
                            if (isMkopiSlamAttack(attackHand)) {
                                sendToServer(
                                        new ClientAttackFlagPayload(ModCombatEvents.ClientAttackFlag.MKOPI_SLAM)
                                );
                            }
                        }
                        return null;
                    }
            );

            attackStartPublisher.getClass().getMethod("register", Object.class).invoke(attackStartPublisher, listener);
        } catch (Throwable ignored) {
        }
    }

    private static void registerBetterCombatAttackHitListener() {
        try {
            Class<?> eventsClass = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents");
            Object attackHitPublisher = eventsClass.getField("ATTACK_HIT").get(null);
            Class<?> listenerClass = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents$PlayerAttackHit");

            Object listener = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    (proxy, method, args) -> {
                        if (!"onPlayerAttackStart".equals(method.getName())
                                || args == null
                                || args.length < 2) {
                            return null;
                        }

                        Object attackHand = args[1];
                        if (attackHand != null) {
                            suppressAttackHitSlashEffect(attackHand);
                        }
                        return null;
                    }
            );

            attackHitPublisher.getClass().getMethod("register", Object.class).invoke(attackHitPublisher, listener);
        } catch (Throwable ignored) {
        }
    }

    private static void suppressAttackHitSlashEffect(Object attackHand) {
        try {
            if (!isBayonetThirdAttack(attackHand)) {
                return;
            }

            Object attack = attackHand.getClass().getMethod("attack").invoke(attackHand);
            if (attack == null) {
                return;
            }

            List<?> noTrail = getNoTrailParticles();
            if (!noTrail.isEmpty()) {
                setAttackField(attack, "trail_particles", noTrail);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void setAttackField(Object attack, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = attack.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(attack, value);
    }

    private static List<?> getNoTrailParticles() {
        List<?> current = noTrailParticles;
        if (current != null) {
            return current;
        }
        List<?> created = createNoTrailParticles();
        noTrailParticles = created;
        return created;
    }

    private static boolean isBayonetAttack(Object attackHand) throws ReflectiveOperationException {
        Object attackItem = attackHand.getClass().getMethod("itemStack").invoke(attackHand);
        return attackItem instanceof ItemStack attackStack && attackStack.is(ModItems.BAYONET.get());
    }

    private static List<?> createNoTrailParticles() {
        try {
            Class<?> particlePlacementClass = Class.forName("net.bettercombat.api.fx.ParticlePlacement");
            Constructor<?> constructor = particlePlacementClass.getConstructor(
                    String.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class,
                    float.class
            );
            Object noTrailParticle = constructor.newInstance(TRAIL_PARTICLE_TYPE_NONE, 0F, 0F, 0F, 0F, 0F, 0F);
            return List.of(noTrailParticle);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static boolean isBayonetThirdAttack(Object attackHand) {
        try {
            if (!isBayonetAttack(attackHand)) {
                return false;
            }

            Object attack = attackHand.getClass().getMethod("attack").invoke(attackHand);
            if (attack == null) {
                return false;
            }

            Object hitbox = attack.getClass().getMethod("hitbox").invoke(attack);
            if (hitbox == null || !BAYONET_GUNSHOT_HITBOX.equals(hitbox.toString())) {
                return false;
            }

            Object animation = attack.getClass().getMethod("animation").invoke(attack);
            if (!BAYONET_GUNSHOT_ANIMATION.equals(animation)) {
                return false;
            }
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isMkopiSlamAttack(Object attackHand) {
        try {
            Object attackItem = attackHand.getClass().getMethod("itemStack").invoke(attackHand);
            if (!(attackItem instanceof ItemStack attackStack) || !attackStack.is(ModItems.MKOPI.get())) {
                return false;
            }

            Object attack = attackHand.getClass().getMethod("attack").invoke(attackHand);
            if (attack == null) {
                return false;
            }

            Object hitbox = attack.getClass().getMethod("hitbox").invoke(attack);
            if (hitbox == null || !MKOPI_SLAM_HITBOX.equals(hitbox.toString())) {
                return false;
            }

            Object animation = attack.getClass().getMethod("animation").invoke(attack);
            return MKOPI_SLAM_ANIMATION.equals(animation);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void spawnBayonetGunshotParticles(LocalPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        double x = player.getX() + look.x * 1.2D;
        double y = player.getEyeY() - 0.2D + look.y * 0.2D;
        double z = player.getZ() + look.z * 1.2D;
        RandomSource random = player.getRandom();

        player.level().addParticle(ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), x, y, z, 0.0, 0.0, 0.0);

        for (int i = 0; i < BURST_COUNT; i++) {
            player.level().addParticle(
                    ParticleTypes.FLAME,
                    x, y, z,
                    (random.nextDouble() - 0.5D) * 0.2D,
                    (random.nextDouble() - 0.5D) * 0.16D,
                    (random.nextDouble() - 0.5D) * 0.2D
            );
            player.level().addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    x, y, z,
                    (random.nextDouble() - 0.5D) * 0.16D,
                    (random.nextDouble() - 0.5D) * 0.14D,
                    (random.nextDouble() - 0.5D) * 0.16D
            );
            player.level().addParticle(
                    ParticleTypes.CRIT,
                    x, y, z,
                    (random.nextDouble() - 0.5D) * 0.2D,
                    (random.nextDouble() - 0.5D) * 0.16D,
                    (random.nextDouble() - 0.5D) * 0.2D
            );
            player.level().addParticle(
                    ParticleTypes.FIREWORK,
                    x, y, z,
                    (random.nextDouble() - 0.5D) * 0.24D,
                    (random.nextDouble() - 0.5D) * 0.18D,
                    (random.nextDouble() - 0.5D) * 0.24D
            );
            player.level().addParticle(
                    ParticleTypes.CLOUD,
                    x, y, z,
                    (random.nextDouble() - 0.5D) * 0.2D,
                    (random.nextDouble() - 0.5D) * 0.14D,
                    (random.nextDouble() - 0.5D) * 0.2D
            );
            player.level().addParticle(
                    ParticleTypes.SMOKE,
                    x, y, z,
                    (random.nextDouble() - 0.5D) * 0.2D,
                    (random.nextDouble() - 0.5D) * 0.16D,
                    (random.nextDouble() - 0.5D) * 0.2D
            );
        }
    }

    private static void sendToServer(CustomPacketPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(payload));
        }
    }
}

package com.fiv.fiverkas_weapons.event.client;

import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
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
    private static final String TRAIL_PARTICLE_TYPE_NONE = "none";
    private static final int BURST_COUNT = 18;
    private static final List<?> NO_TRAIL_PARTICLES = createNoTrailParticles();

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
        if (event.getEntity().hasEffect(ModEffects.CERULEAN_SHROUD)) {
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
                        if (attackHand != null && isBayonetThirdAttack(attackHand)) {
                            spawnBayonetGunshotParticles(player);
                        }
                        return null;
                    }
            );

            attackStartPublisher.getClass().getMethod("register", Object.class).invoke(attackStartPublisher, listener);
        } catch (ReflectiveOperationException ignored) {
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
        } catch (ReflectiveOperationException ignored) {
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

            if (!NO_TRAIL_PARTICLES.isEmpty()) {
                setAttackField(attack, "trail_particles", NO_TRAIL_PARTICLES);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void setAttackField(Object attack, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = attack.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(attack, value);
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
        } catch (ReflectiveOperationException ignored) {
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

    private static void spawnBayonetGunshotParticles(LocalPlayer player) {
        Vec3 look = player.getLookAngle().normalize();
        double x = player.getX() + look.x * 1.2D;
        double y = player.getEyeY() - 0.2D + look.y * 0.2D;
        double z = player.getZ() + look.z * 1.2D;
        RandomSource random = player.getRandom();

        player.level().addParticle(ParticleTypes.FLASH, x, y, z, 0.0, 0.0, 0.0);

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
}

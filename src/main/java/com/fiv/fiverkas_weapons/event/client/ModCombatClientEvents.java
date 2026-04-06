package com.fiv.fiverkas_weapons.event.client;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.network.BayonetComboAttackPayload;
import com.fiv.fiverkas_weapons.network.BayonetMuzzleFlashPayload;
import com.fiv.fiverkas_weapons.network.ClientAttackFlagPayload;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.systems.RenderSystem;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModCombatClientEvents {
    private static final String BAYONET_GUNSHOT_ANIMATION = "fweapons:bayonet_no_swing";
    private static final String BAYONET_IMPACT_ANIMATION = "fweapons:bayonet_impact";
    private static final String BAYONET_GUNSHOT_HITBOX = "FORWARD_BOX";
    private static final String MKOPI_SLAM_ANIMATION = "bettercombat:two_handed_slam";
    private static final String MKOPI_SLAM_HITBOX = "VERTICAL_PLANE";
    private static final String DUSK_THIRD_ANIMATION = "bettercombat:dual_handed_stab";
    private static final String DUSK_THIRD_HITBOX = "FORWARD_BOX";
    private static final String TRAIL_PARTICLE_TYPE_NONE = "none";
    private static final int BURST_COUNT = 18;
    private static final List<?> NO_TRAIL_PARTICLES = createNoTrailParticles();
    private static final ResourceLocation[] IMPACT_FRAMES = {
            ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "textures/gui/impact/frame0.png"),
            ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "textures/gui/impact/frame1.png"),
            ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "textures/gui/impact/frame2.png")
    };
    private static final long[] IMPACT_FRAME_DURATIONS_MS = {300L, 300L, 400L};
    private static final int IMPACT_FRAME_WIDTH = 1920;
    private static final int IMPACT_FRAME_HEIGHT = 1080;
    private static final long IMPACT_FRAME_DURATION_MS = 1000L;
    private static final float IMPACT_FRAME_SCALE = 1.4F;
    private static final Map<MethodKey, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<FieldKey, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static boolean bayonetImpactFrameActive = false;
    private static long bayonetImpactFrameStartTime = 0L;

    private record MethodKey(Class<?> type, String name) {
    }

    private record FieldKey(Class<?> type, String name) {
    }

    private ModCombatClientEvents() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ModCombatClientEvents::init);
    }

    private static void init() {
        registerClientRenderHooks();
        registerItemProperties();
        registerBetterCombatAttackStartListener();
        registerBetterCombatAttackHitListener();
    }

    private static void registerClientRenderHooks() {
        NeoForge.EVENT_BUS.addListener(RenderPlayerEvent.Pre.class, ModCombatClientEvents::onRenderPlayerPre);
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, ModCombatClientEvents::onRenderGuiPost);
    }

    private static void registerItemProperties() {
        ItemProperties.register(
                ModItems.THE_FOOL.get(),
                ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != stack) {
                        return 0.0F;
                    }
                    return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
                }
        );
        ItemProperties.register(
                ModItems.THE_FOOL.get(),
                ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
        );
    }

    private static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.ceruleanShroudHolder())) {
            event.setCanceled(true);
        }
    }

    public static void triggerBayonetImpactFrame() {
        bayonetImpactFrameActive = true;
        bayonetImpactFrameStartTime = System.currentTimeMillis();
    }

    private static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!bayonetImpactFrameActive) {
            return;
        }
        long elapsed = System.currentTimeMillis() - bayonetImpactFrameStartTime;
        if (elapsed >= IMPACT_FRAME_DURATION_MS) {
            bayonetImpactFrameActive = false;
            return;
        }
        long frameTime = elapsed;
        int frameIndex = 0;
        for (int i = 0; i < IMPACT_FRAME_DURATIONS_MS.length; i++) {
            if (frameTime < IMPACT_FRAME_DURATIONS_MS[i]) {
                frameIndex = i;
                break;
            }
            frameTime -= IMPACT_FRAME_DURATIONS_MS[i];
            frameIndex = i + 1;
        }
        if (frameIndex >= IMPACT_FRAMES.length) {
            frameIndex = IMPACT_FRAMES.length - 1;
        }
        ResourceLocation frame = IMPACT_FRAMES[frameIndex];

        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
        float fitScale = Math.min(
                width / (float) IMPACT_FRAME_WIDTH,
                height / (float) IMPACT_FRAME_HEIGHT
        );
        float scale = fitScale * IMPACT_FRAME_SCALE;
        int drawWidth = Math.round(IMPACT_FRAME_WIDTH * scale);
        int drawHeight = Math.round(IMPACT_FRAME_HEIGHT * scale);
        int x = (width - drawWidth) / 2;
        int y = (height - drawHeight) / 2;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(x, y, 0.0F);
        event.getGuiGraphics().pose().scale(scale, scale, 1.0F);
        event.getGuiGraphics().blit(
                frame,
                0,
                0,
                0.0F,
                0.0F,
                IMPACT_FRAME_WIDTH,
                IMPACT_FRAME_HEIGHT,
                IMPACT_FRAME_WIDTH,
                IMPACT_FRAME_HEIGHT
        );
        event.getGuiGraphics().pose().popPose();
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
                            boolean bayonetImpactAttack = isBayonetImpactAttack(attackHand);
                            if (bayonetImpactAttack) {
                                PacketDistributor.sendToServer(new BayonetComboAttackPayload());
                                triggerBayonetImpactFrame();
                            }
                            if (bayonetImpactAttack || isBayonetGunshotAttack(attackHand)) {
                                spawnBayonetGunshotParticles(player);
                                PacketDistributor.sendToServer(new BayonetMuzzleFlashPayload());
                            }
                            if (isMkopiSlamAttack(attackHand)) {
                                PacketDistributor.sendToServer(
                                        new ClientAttackFlagPayload(ModCombatEvents.ClientAttackFlag.MKOPI_SLAM)
                                );
                            }
                            if (isDuskThirdAttack(attackHand)) {
                                PacketDistributor.sendToServer(
                                        new ClientAttackFlagPayload(ModCombatEvents.ClientAttackFlag.DUSK_THIRD)
                                );
                            }
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
                        if (!"onPlayerAttackHit".equals(method.getName())
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
            if (!isBayonetGunshotAttack(attackHand)) {
                return;
            }

            Object attack = invokeCached(attackHand, "attack");
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
        Field field = getCachedField(attack.getClass(), fieldName);
        field.set(attack, value);
    }

    private static boolean isBayonetAttack(Object attackHand) throws ReflectiveOperationException {
        Object attackItem = invokeCached(attackHand, "itemStack");
        return attackItem instanceof ItemStack attackStack && attackStack.is(ModItems.BAYONET.get());
    }

    private static boolean isDuskAttack(Object attackHand) throws ReflectiveOperationException {
        Object attackItem = invokeCached(attackHand, "itemStack");
        return attackItem instanceof ItemStack attackStack && attackStack.is(ModItems.DUSK.get());
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

    private static boolean isBayonetGunshotAttack(Object attackHand) {
        try {
            if (!isBayonetAttack(attackHand)) {
                return false;
            }

            Object attack = invokeCached(attackHand, "attack");
            if (attack == null) {
                return false;
            }

            Object hitbox = invokeCached(attack, "hitbox");
            if (hitbox == null || !BAYONET_GUNSHOT_HITBOX.equals(hitbox.toString())) {
                return false;
            }

            Object animation = invokeCached(attack, "animation");
            if (!BAYONET_GUNSHOT_ANIMATION.equals(animation) && !BAYONET_IMPACT_ANIMATION.equals(animation)) {
                return false;
            }
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isBayonetImpactAttack(Object attackHand) {
        try {
            if (!isBayonetAttack(attackHand)) {
                return false;
            }

            Object attack = invokeCached(attackHand, "attack");
            if (attack == null) {
                return false;
            }

            Object hitbox = invokeCached(attack, "hitbox");
            if (hitbox == null || !BAYONET_GUNSHOT_HITBOX.equals(hitbox.toString())) {
                return false;
            }

            Object animation = invokeCached(attack, "animation");
            return BAYONET_IMPACT_ANIMATION.equals(animation);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isDuskThirdAttack(Object attackHand) {
        try {
            if (!isDuskAttack(attackHand)) {
                return false;
            }

            Object attack = invokeCached(attackHand, "attack");
            if (attack == null) {
                return false;
            }

            Object hitbox = invokeCached(attack, "hitbox");
            if (hitbox == null || !DUSK_THIRD_HITBOX.equals(hitbox.toString())) {
                return false;
            }

            Object animation = invokeCached(attack, "animation");
            if (!DUSK_THIRD_ANIMATION.equals(animation)) {
                return false;
            }
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isMkopiSlamAttack(Object attackHand) {
        try {
            Object attackItem = invokeCached(attackHand, "itemStack");
            if (!(attackItem instanceof ItemStack attackStack) || !attackStack.is(ModItems.MKOPI.get())) {
                return false;
            }

            Object attack = invokeCached(attackHand, "attack");
            if (attack == null) {
                return false;
            }

            Object hitbox = invokeCached(attack, "hitbox");
            if (hitbox == null || !MKOPI_SLAM_HITBOX.equals(hitbox.toString())) {
                return false;
            }

            Object animation = invokeCached(attack, "animation");
            return MKOPI_SLAM_ANIMATION.equals(animation);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Object invokeCached(Object target, String methodName) throws ReflectiveOperationException {
        MethodKey key = new MethodKey(target.getClass(), methodName);
        Method method = METHOD_CACHE.get(key);
        if (method == null) {
            method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            METHOD_CACHE.put(key, method);
        }
        return method.invoke(target);
    }

    private static Field getCachedField(Class<?> type, String fieldName) throws ReflectiveOperationException {
        FieldKey key = new FieldKey(type, fieldName);
        Field field = FIELD_CACHE.get(key);
        if (field == null) {
            field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            FIELD_CACHE.put(key, field);
        }
        return field;
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

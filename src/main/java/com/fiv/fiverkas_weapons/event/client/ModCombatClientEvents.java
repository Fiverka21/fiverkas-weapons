package com.fiv.fiverkas_weapons.event.client;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.network.BayonetComboAttackPayload;
import com.fiv.fiverkas_weapons.network.BayonetMuzzleFlashPayload;
import com.fiv.fiverkas_weapons.network.ClientAttackFlagPayload;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import com.fiv.fiverkas_weapons.util.CompatIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Function;

public final class ModCombatClientEvents {
    private static final String BAYONET_GUNSHOT_ANIMATION = "fweapons:bayonet_no_swing";
    private static final String BAYONET_IMPACT_ANIMATION = "fweapons:bayonet_impact";
    private static final String BAYONET_GUNSHOT_HITBOX = "FORWARD_BOX";
    private static final String MKOPI_SLAM_ANIMATION = "bettercombat:two_handed_slam";
    private static final String MKOPI_SLAM_HITBOX = "VERTICAL_PLANE";
    private static final String DUSK_THIRD_ANIMATION_PRIMARY = "bettercombat:dual_handed_stab";
    private static final String DUSK_THIRD_ANIMATION_FALLBACK = "bettercombat:one_handed_stab";
    private static final String DUSK_THIRD_HITBOX = "FORWARD_BOX";
    private static final String TRAIL_PARTICLE_TYPE_NONE = "none";
    private static final String[] ITEM_PROPERTIES_CLASS_NAMES = new String[]{
            "net.minecraft.client.renderer.item.ItemProperties",
            "net.minecraft.client.renderer.item.properties.ItemProperties"
    };
    private static final String[] ITEM_PROPERTY_FUNCTION_CLASS_NAMES = new String[]{
            "net.minecraft.client.renderer.item.ItemPropertyFunction",
            "net.minecraft.client.renderer.item.properties.ItemPropertyFunction"
    };
    private static final int BURST_COUNT = 18;
    private static final double BAYONET_RANGED_EXTRA_RANGE = 24.0D;
    private static final Identifier[] IMPACT_FRAMES = {
            Identifier.fromNamespaceAndPath(FiverkasWeapons.MODID, "textures/gui/impact/frame0.png"),
            Identifier.fromNamespaceAndPath(FiverkasWeapons.MODID, "textures/gui/impact/frame1.png"),
            Identifier.fromNamespaceAndPath(FiverkasWeapons.MODID, "textures/gui/impact/frame2.png")
    };
    private static final long[] IMPACT_FRAME_DURATIONS_MS = {300L, 300L, 400L};
    private static final int IMPACT_FRAME_WIDTH = 1920;
    private static final int IMPACT_FRAME_HEIGHT = 1080;
    private static final long IMPACT_FRAME_DURATION_MS = 1000L;
    private static volatile List<?> noTrailParticles;
    private static boolean bayonetImpactFrameActive = false;
    private static long bayonetImpactFrameStartTime = 0L;

    @FunctionalInterface
    private interface ItemPropertyGetter {
        float call(ItemStack stack, ClientLevel level, LivingEntity entity, int seed);
    }

    private ModCombatClientEvents() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ModCombatClientEvents::init);
    }

    private static void init() {
        registerClientRenderHooks();
        registerItemProperties();
        registerAttackRangeExtension();
        registerBetterCombatAttackStartListener();
        registerBetterCombatAttackHitListener();
    }

    private static void registerClientRenderHooks() {
        NeoForge.EVENT_BUS.addListener(ModCombatClientEvents::onRenderPlayerPre);
        NeoForge.EVENT_BUS.addListener(ModCombatClientEvents::onRenderGuiPost);
    }

    private static void registerItemProperties() {
        registerItemProperty(
                ModItems.THE_FOOL.get(),
                "pull",
                (stack, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != stack) {
                        return 0.0F;
                    }
                    return (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
                }
        );
        registerItemProperty(
                ModItems.THE_FOOL.get(),
                "pulling",
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
        );
    }

    private static void registerItemProperty(Item item, String path, ItemPropertyGetter getter) {
        Object id = CompatIds.id("minecraft", path);
        Class<?> propertiesClass = getItemPropertiesClass();
        Class<?> functionClass = getItemPropertyFunctionClass();
        if (propertiesClass == null || functionClass == null) {
            return;
        }
        Object function = createItemPropertyFunction(functionClass, getter);
        if (function == null) {
            return;
        }
        try {
            Method register = findItemPropertiesRegister(propertiesClass, id, functionClass);
            if (register != null) {
                register.invoke(null, item, id, function);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerAttackRangeExtension() {
        try {
            Class<?> extensionsClass = Class.forName("net.bettercombat.api.client.AttackRangeExtensions");
            Class<?> contextClass = Class.forName("net.bettercombat.api.client.AttackRangeExtensions$Context");
            Class<?> modifierClass = Class.forName("net.bettercombat.api.client.AttackRangeExtensions$Modifier");
            Class<? extends Enum> operationClass =
                    (Class<? extends Enum>) Class.forName("net.bettercombat.api.client.AttackRangeExtensions$Operation");

            Method register = extensionsClass.getMethod("register", Function.class);
            Method playerGetter = contextClass.getMethod("player");
            Constructor<?> modifierCtor = modifierClass.getConstructor(double.class, operationClass);
            Object addOperation = Enum.valueOf(operationClass, "ADD");
            Object neutralModifier = modifierCtor.newInstance(0.0D, addOperation);

            Function<Object, Object> source = context -> {
                double extraRange = 0.0D;
                try {
                    Object playerObj = playerGetter.invoke(context);
                    if (playerObj instanceof LivingEntity player && isBayonetRangedCurrentAttack(player)) {
                        extraRange = BAYONET_RANGED_EXTRA_RANGE;
                    }
                } catch (Throwable ignored) {
                }
                try {
                    return modifierCtor.newInstance(extraRange, addOperation);
                } catch (Throwable ignored) {
                    return neutralModifier;
                }
            };

            register.invoke(null, source);
        } catch (Throwable ignored) {
        }
    }

    private static Object createItemPropertyFunction(Class<?> functionClass, ItemPropertyGetter getter) {
        return Proxy.newProxyInstance(
                functionClass.getClassLoader(),
                new Class<?>[]{functionClass},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(getter, args);
                    }
                    if (args != null && args.length == 4 && args[0] instanceof ItemStack stack) {
                        ClientLevel level = args[1] instanceof ClientLevel clientLevel ? clientLevel : null;
                        LivingEntity entity = args[2] instanceof LivingEntity living ? living : null;
                        int seed = args[3] instanceof Integer value ? value : 0;
                        return getter.call(stack, level, entity, seed);
                    }
                    return 0.0F;
                }
        );
    }

    private static Method findItemPropertiesRegister(Class<?> propertiesClass, Object id, Class<?> functionClass) {
        Class<?> idClass = id.getClass();
        for (Method method : propertiesClass.getMethods()) {
            if (!method.getName().equals("register") || method.getParameterCount() != 3) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (!Item.class.isAssignableFrom(params[0])) {
                continue;
            }
            if (!params[1].isAssignableFrom(idClass) && !idClass.isAssignableFrom(params[1])) {
                continue;
            }
            if (!params[2].isAssignableFrom(functionClass) && !functionClass.isAssignableFrom(params[2])) {
                continue;
            }
            return method;
        }
        return null;
    }

    private static Class<?> getItemPropertiesClass() {
        return findClass(ITEM_PROPERTIES_CLASS_NAMES);
    }

    private static Class<?> getItemPropertyFunctionClass() {
        return findClass(ITEM_PROPERTY_FUNCTION_CLASS_NAMES);
    }

    private static Class<?> findClass(String[] candidates) {
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity living && living.hasEffect(ModEffects.CERULEAN_SHROUD)) {
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
        if (frameIndex >= IMPACT_FRAME_DURATIONS_MS.length) {
            frameIndex = IMPACT_FRAME_DURATIONS_MS.length - 1;
        }
        Identifier frame = IMPACT_FRAMES[frameIndex];
        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
        event.getGuiGraphics().blit(
                RenderPipelines.GUI_TEXTURED,
                frame,
                0,
                0,
                0.0F,
                0.0F,
                width,
                height,
                IMPACT_FRAME_WIDTH,
                IMPACT_FRAME_HEIGHT,
                IMPACT_FRAME_WIDTH,
                IMPACT_FRAME_HEIGHT
        );
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
                            if (isBayonetImpactAttack(attackHand)) {
                                sendToServer(new BayonetComboAttackPayload());
                                triggerBayonetImpactFrame();
                            }
                            if (isBayonetGunshotAttack(attackHand)) {
                                spawnBayonetGunshotParticles(player);
                                sendToServer(new BayonetMuzzleFlashPayload());
                            }
                            if (isMkopiSlamAttack(attackHand)) {
                                sendToServer(
                                        new ClientAttackFlagPayload(ModCombatEvents.ClientAttackFlag.MKOPI_SLAM)
                                );
                            }
                            if (isDuskThirdAttack(attackHand)) {
                                sendToServer(
                                        new ClientAttackFlagPayload(ModCombatEvents.ClientAttackFlag.DUSK_THIRD)
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
                        if (!"onPlayerAttackHit".equals(method.getName())
                                || args == null
                                || args.length < 2) {
                            return null;
                        }

                        Object attackHand = args[1];
                        if (attackHand != null) {
                            if (isBayonetImpactAttack(attackHand)) {
                                triggerBayonetImpactFrame();
                            }
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
            if (!isBayonetGunshotAttack(attackHand)) {
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

    private static boolean isDuskAttack(Object attackHand) throws ReflectiveOperationException {
        Object attackItem = attackHand.getClass().getMethod("itemStack").invoke(attackHand);
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
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static boolean isBayonetGunshotAttack(Object attackHand) {
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
            if (!BAYONET_GUNSHOT_ANIMATION.equals(animation) && !BAYONET_IMPACT_ANIMATION.equals(animation)) {
                return false;
            }
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isBayonetRangedCurrentAttack(LivingEntity attacker) {
        try {
            Object currentAttack = attacker.getClass().getMethod("getCurrentAttack").invoke(attacker);
            return currentAttack != null && isBayonetGunshotAttack(currentAttack);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isBayonetImpactAttack(Object attackHand) {
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

            Object attack = attackHand.getClass().getMethod("attack").invoke(attackHand);
            if (attack == null) {
                return false;
            }

            Object hitbox = attack.getClass().getMethod("hitbox").invoke(attack);
            if (hitbox == null || !DUSK_THIRD_HITBOX.equals(hitbox.toString())) {
                return false;
            }

            Object animation = attack.getClass().getMethod("animation").invoke(attack);
            return isDuskThirdAnimation(animation);
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

    private static boolean isDuskThirdAnimation(Object animation) {
        if (animation == null) {
            return false;
        }
        String value = animation.toString();
        return DUSK_THIRD_ANIMATION_PRIMARY.equals(value) || DUSK_THIRD_ANIMATION_FALLBACK.equals(value);
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

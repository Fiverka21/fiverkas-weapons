package com.fiv.fiverkas_weapons.event.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.event.ModCombatEvents;
import com.fiv.fiverkas_weapons.item.HCBowItem;
import com.fiv.fiverkas_weapons.network.BayonetComboAttackPayload;
import com.fiv.fiverkas_weapons.network.BayonetMuzzleFlashPayload;
import com.fiv.fiverkas_weapons.network.ClientAttackFlagPayload;
import com.fiv.fiverkas_weapons.registry.ModItems;
import com.fiv.fiverkas_weapons.registry.ModEffects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ModCombatClientEvents {
    private static final int ANTEM_THIRD_PATTERN_INDEX = 2;
    private static final int ANTEM_FOURTH_PATTERN_INDEX = 3;
    private static final int ANTEM_SEVENTH_PATTERN_INDEX = 6;
    private static final long ANTEM_HEAT_TIMEOUT_MS = 2000L;
    private static final String ANTEM_PATTERN_RESOURCE_PATH =
            "data/" + FiverkasWeapons.MODID + "/weapon_attributes/antem.json";
    private static final String BAYONET_GUNSHOT_ANIMATION = "fweapons:bayonet_no_swing";
    private static final String BAYONET_IMPACT_ANIMATION = "fweapons:bayonet_impact";
    private static final String BAYONET_GUNSHOT_HITBOX = "FORWARD_BOX";
    private static final String MKOPI_SLAM_ANIMATION = "bettercombat:two_handed_slam";
    private static final String MKOPI_SLAM_HITBOX = "VERTICAL_PLANE";
    private static final String DUSK_THIRD_ANIMATION = "bettercombat:dual_handed_stab";
    private static final String DUSK_THIRD_HITBOX = "FORWARD_BOX";
    private static final String TRAIL_PARTICLE_TYPE_NONE = "none";
    private static final int BURST_COUNT = 18;
    private static final int ANTEM_FIRE_PARTICLE_COUNT = 48;
    private static final int ANTEM_WIND_PARTICLE_COUNT = 24;
    private static final double MKOPI_RENDER_SHAKE_TRANSLATE = 0.055D;
    private static final float MKOPI_RENDER_SHAKE_ROTATE_DEGREES = 3.2F;
    private static final List<?> NO_TRAIL_PARTICLES = createNoTrailParticles();
    private static final List<AttackSignature> ANTEM_ATTACK_PATTERN = loadAntemAttackPattern();
    private static final Map<MethodKey, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, AntemPatternState> ANTEM_PATTERN_STATES = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> ANTEM_HEAT_ACTIVE = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> ANTEM_HEAT_STARTED_AT_MS = new ConcurrentHashMap<>();
    private static final Set<Integer> MKOPI_SHAKE_RENDER_PUSHED = ConcurrentHashMap.newKeySet();
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
    private static boolean bayonetImpactFrameActive = false;
    private static long bayonetImpactFrameStartTime = 0L;

    private record MethodKey(Class<?> type, String name) {
    }

    private record AttackHandInfo(ItemStack stack, Object attack, String hitbox, String animation) {
    }

    private record AttackSignature(String hitbox, String animation) {
    }

    private static final class AntemPatternState {
        private int patternIndex = -1;
        private boolean heatActive = false;

        private void reset() {
            patternIndex = -1;
            heatActive = false;
        }
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
        NeoForge.EVENT_BUS.addListener(ModCombatClientEvents::onRenderPlayerPre);
        NeoForge.EVENT_BUS.addListener(ModCombatClientEvents::onRenderPlayerPost);
        NeoForge.EVENT_BUS.addListener(ModCombatClientEvents::onRenderGuiPost);
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
        ItemProperties.register(
                ModItems.HCBOW.get(),
                ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null) {
                        return 0.0F;
                    }
                    return CrossbowItem.isCharged(stack)
                            ? 0.0F
                            : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks())
                            / (float) HCBowItem.getChargeDuration(stack, entity);
                }
        );
        ItemProperties.register(
                ModItems.HCBOW.get(),
                ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) ->
                        entity != null
                                && entity.isUsingItem()
                                && entity.getUseItem() == stack
                                && !CrossbowItem.isCharged(stack)
                                ? 1.0F
                                : 0.0F
        );
        ItemProperties.register(
                ModItems.HCBOW.get(),
                ResourceLocation.withDefaultNamespace("charged"),
                (stack, level, entity, seed) -> CrossbowItem.isCharged(stack) ? 1.0F : 0.0F
        );
        ItemProperties.register(
                ModItems.HCBOW.get(),
                ResourceLocation.withDefaultNamespace("firework"),
                (stack, level, entity, seed) -> {
                    ChargedProjectiles chargedProjectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
                    return chargedProjectiles != null && chargedProjectiles.contains(Items.FIREWORK_ROCKET)
                            ? 1.0F
                            : 0.0F;
                }
        );
        ItemProperties.register(
                ModItems.ANTEM.get(),
                ResourceLocation.fromNamespaceAndPath(FiverkasWeapons.MODID, "antem_heat"),
                (stack, level, entity, seed) -> isAntemHeatActive(stack, entity) ? 1.0F : 0.0F
        );
        ItemProperties.register(
                ModItems.DSHIELD.get(),
                ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
        );
    }

    private static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.CERULEAN_SHROUD)) {
            event.setCanceled(true);
            return;
        }
        if (isUsingMkopi(event.getEntity())) {
            applyMkopiRenderShake(event);
        }
    }

    private static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (MKOPI_SHAKE_RENDER_PUSHED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    private static boolean isUsingMkopi(Player player) {
        return player.isUsingItem() && player.getUseItem().is(ModItems.MKOPI.get());
    }

    private static void applyMkopiRenderShake(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        float time = (float) player.tickCount + event.getPartialTick();
        double x = Math.sin(time * 3.9F) * MKOPI_RENDER_SHAKE_TRANSLATE;
        double y = Math.sin(time * 5.1F) * MKOPI_RENDER_SHAKE_TRANSLATE * 0.45D;
        double z = Math.cos(time * 4.4F) * MKOPI_RENDER_SHAKE_TRANSLATE;
        float zRot = (float) Math.sin(time * 5.7F) * MKOPI_RENDER_SHAKE_ROTATE_DEGREES;
        float xRot = (float) Math.cos(time * 4.8F) * MKOPI_RENDER_SHAKE_ROTATE_DEGREES * 0.55F;

        event.getPoseStack().pushPose();
        event.getPoseStack().translate(x, y, z);
        event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(zRot));
        event.getPoseStack().mulPose(Axis.XP.rotationDegrees(xRot));
        MKOPI_SHAKE_RENDER_PUSHED.add(player.getId());
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
                            AttackHandInfo attackInfo;
                            try {
                                attackInfo = readAttackHandInfo(attackHand);
                            } catch (ReflectiveOperationException ignored) {
                                return null;
                            }
                            if (attackInfo == null) {
                                return null;
                            }

                            if (isBayonetImpactAttack(attackInfo)) {
                                PacketDistributor.sendToServer(new BayonetComboAttackPayload());
                                triggerBayonetImpactFrame();
                            }
                            int antemPatternIndex = updateAntemPatternState(player, attackInfo);
                            if (antemPatternIndex == ANTEM_THIRD_PATTERN_INDEX) {
                                spawnAntemFireParticles(player);
                            }
                            if (antemPatternIndex == ANTEM_SEVENTH_PATTERN_INDEX) {
                                spawnAntemWindParticles(player);
                            }
                            if (isBayonetGunshotAttack(attackInfo)) {
                                spawnBayonetGunshotParticles(player);
                                PacketDistributor.sendToServer(new BayonetMuzzleFlashPayload());
                            }
                            if (isMkopiSlamAttack(attackInfo)) {
                                PacketDistributor.sendToServer(
                                        new ClientAttackFlagPayload(ModCombatEvents.ClientAttackFlag.MKOPI_SLAM)
                                );
                            }
                            if (isDuskThirdAttack(attackInfo)) {
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
            AttackHandInfo attackInfo = readAttackHandInfo(attackHand);
            if (attackInfo == null || !isBayonetGunshotAttack(attackInfo)) {
                return;
            }
            if (attackInfo.attack() == null) {
                return;
            }

            if (!NO_TRAIL_PARTICLES.isEmpty()) {
                setAttackField(attackInfo.attack(), "trail_particles", NO_TRAIL_PARTICLES);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void setAttackField(Object attack, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = attack.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(attack, value);
    }

    private static boolean isAntemHeatActive(ItemStack stack, Object entity) {
        if (!(entity instanceof LivingEntity livingEntity) || !stack.is(ModItems.ANTEM.get())) {
            return false;
        }
        if (ANTEM_ATTACK_PATTERN.size() <= ANTEM_THIRD_PATTERN_INDEX) {
            return false;
        }
        int entityId = livingEntity.getId();
        if (!ANTEM_HEAT_ACTIVE.getOrDefault(entityId, false)) {
            return false;
        }
        Long startedAtMs = ANTEM_HEAT_STARTED_AT_MS.get(entityId);
        if (startedAtMs == null) {
            ANTEM_HEAT_ACTIVE.put(entityId, false);
            return false;
        }
        if (System.currentTimeMillis() - startedAtMs >= ANTEM_HEAT_TIMEOUT_MS) {
            ANTEM_HEAT_ACTIVE.put(entityId, false);
            ANTEM_HEAT_STARTED_AT_MS.remove(entityId);
            return false;
        }
        return true;
    }

    private static int updateAntemPatternState(LivingEntity entity, AttackHandInfo attackInfo) {
        if (ANTEM_ATTACK_PATTERN.size() <= ANTEM_THIRD_PATTERN_INDEX) {
            return -1;
        }

        int entityId = entity.getId();
        AntemPatternState state = ANTEM_PATTERN_STATES.computeIfAbsent(entityId, id -> new AntemPatternState());
        boolean wasHeatActive = state.heatActive;
        if (attackInfo == null || attackInfo.attack() == null || !attackInfo.stack().is(ModItems.ANTEM.get())) {
            state.reset();
            ANTEM_HEAT_ACTIVE.put(entityId, wasHeatActive);
            return -1;
        }

        AttackSignature signature = new AttackSignature(attackInfo.hitbox(), attackInfo.animation());
        int nextPatternIndex = advanceAntemPattern(state.patternIndex, signature);

        state.patternIndex = nextPatternIndex;

        if (wasHeatActive && isPatternIndexMatch(nextPatternIndex, ANTEM_FOURTH_PATTERN_INDEX, signature)) {
            // Heat window closes on the normal 4th attack start.
            state.heatActive = false;
            ANTEM_HEAT_STARTED_AT_MS.remove(entityId);
        }

        if (!state.heatActive && isPatternIndexMatch(nextPatternIndex, ANTEM_THIRD_PATTERN_INDEX, signature)) {
            state.heatActive = true;
            ANTEM_HEAT_STARTED_AT_MS.put(entityId, System.currentTimeMillis());
        }

        ANTEM_HEAT_ACTIVE.put(entityId, state.heatActive);
        return nextPatternIndex;
    }

    private static boolean isPatternIndexMatch(int index, int expectedIndex, AttackSignature signature) {
        return index == expectedIndex
                && ANTEM_ATTACK_PATTERN.size() > expectedIndex
                && matchesAttackSignature(signature, ANTEM_ATTACK_PATTERN.get(expectedIndex));
    }

    private static int advanceAntemPattern(int currentPatternIndex, AttackSignature signature) {
        int patternSize = ANTEM_ATTACK_PATTERN.size();
        if (patternSize == 0 || signature == null) {
            return -1;
        }

        int expectedNextIndex = (currentPatternIndex + 1) % patternSize;
        if (matchesAttackSignature(signature, ANTEM_ATTACK_PATTERN.get(expectedNextIndex))) {
            return expectedNextIndex;
        }
        if (matchesAttackSignature(signature, ANTEM_ATTACK_PATTERN.get(0))) {
            return 0;
        }
        return -1;
    }

    private static boolean matchesAttackSignature(AttackSignature left, AttackSignature right) {
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.hitbox(), right.hitbox())
                && Objects.equals(left.animation(), right.animation());
    }

    private static List<AttackSignature> loadAntemAttackPattern() {
        try (InputStream stream = ModCombatClientEvents.class.getClassLoader()
                .getResourceAsStream(ANTEM_PATTERN_RESOURCE_PATH)) {
            if (stream == null) {
                return List.of();
            }
            JsonElement rootElement = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!rootElement.isJsonObject()) {
                return List.of();
            }
            JsonObject root = rootElement.getAsJsonObject();
            JsonObject attributes = root.getAsJsonObject("attributes");
            if (attributes == null) {
                return List.of();
            }
            JsonArray attacks = attributes.getAsJsonArray("attacks");
            if (attacks == null) {
                return List.of();
            }

            List<AttackSignature> pattern = new ArrayList<>(attacks.size());
            for (JsonElement attackElement : attacks) {
                if (!attackElement.isJsonObject()) {
                    continue;
                }
                JsonObject attack = attackElement.getAsJsonObject();
                String hitbox = readJsonString(attack, "hitbox");
                String animation = readJsonString(attack, "animation");
                pattern.add(new AttackSignature(hitbox, animation));
            }
            return pattern;
        } catch (RuntimeException | java.io.IOException ignored) {
            return List.of();
        }
    }

    private static String readJsonString(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
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

    private static AttackHandInfo readAttackHandInfo(Object attackHand) throws ReflectiveOperationException {
        Object attackItem = invokeCached(attackHand, "itemStack");
        if (!(attackItem instanceof ItemStack attackStack)) {
            return null;
        }
        Object attack = invokeCached(attackHand, "attack");
        if (attack == null) {
            return new AttackHandInfo(attackStack, null, null, null);
        }
        Object hitbox = invokeCached(attack, "hitbox");
        Object animation = invokeCached(attack, "animation");
        return new AttackHandInfo(
                attackStack,
                attack,
                hitbox == null ? null : hitbox.toString(),
                animation == null ? null : animation.toString()
        );
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

    private static boolean isBayonetAttack(AttackHandInfo attackInfo) {
        return attackInfo != null && attackInfo.stack().is(ModItems.BAYONET.get());
    }

    private static boolean isDuskAttack(AttackHandInfo attackInfo) {
        return attackInfo != null && attackInfo.stack().is(ModItems.DUSK.get());
    }

    private static boolean isBayonetGunshotAttack(AttackHandInfo attackInfo) {
        if (!isBayonetAttack(attackInfo)) {
            return false;
        }
        if (attackInfo.hitbox() == null || !BAYONET_GUNSHOT_HITBOX.equals(attackInfo.hitbox())) {
            return false;
        }
        return BAYONET_GUNSHOT_ANIMATION.equals(attackInfo.animation())
                || BAYONET_IMPACT_ANIMATION.equals(attackInfo.animation());
    }

    private static boolean isBayonetImpactAttack(AttackHandInfo attackInfo) {
        if (!isBayonetAttack(attackInfo)) {
            return false;
        }
        if (attackInfo.hitbox() == null || !BAYONET_GUNSHOT_HITBOX.equals(attackInfo.hitbox())) {
            return false;
        }
        return BAYONET_IMPACT_ANIMATION.equals(attackInfo.animation());
    }

    private static boolean isDuskThirdAttack(AttackHandInfo attackInfo) {
        if (!isDuskAttack(attackInfo)) {
            return false;
        }
        if (attackInfo.hitbox() == null || !DUSK_THIRD_HITBOX.equals(attackInfo.hitbox())) {
            return false;
        }
        return DUSK_THIRD_ANIMATION.equals(attackInfo.animation());
    }

    private static boolean isMkopiSlamAttack(AttackHandInfo attackInfo) {
        if (attackInfo == null || !attackInfo.stack().is(ModItems.MKOPI.get())) {
            return false;
        }
        if (attackInfo.hitbox() == null || !MKOPI_SLAM_HITBOX.equals(attackInfo.hitbox())) {
            return false;
        }
        return MKOPI_SLAM_ANIMATION.equals(attackInfo.animation());
    }

    private static void spawnAntemFireParticles(LocalPlayer player) {
        RandomSource random = player.getRandom();
        double y = player.getY() + 1.0D;

        for (int i = 0; i < ANTEM_FIRE_PARTICLE_COUNT; i++) {
            double angle = (Math.PI * 2.0D * i) / ANTEM_FIRE_PARTICLE_COUNT;
            double radius = 0.65D + random.nextDouble() * 0.55D;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double velocityScale = 0.045D + random.nextDouble() * 0.035D;
            double dx = Math.cos(angle) * velocityScale;
            double dz = Math.sin(angle) * velocityScale;
            double dy = 0.03D + random.nextDouble() * 0.06D;

            player.level().addParticle(ParticleTypes.FLAME, x, y + random.nextDouble() * 0.5D, z, dx, dy, dz);
            if ((i & 1) == 0) {
                player.level().addParticle(ParticleTypes.SMALL_FLAME, x, y + 0.25D, z, dx * 0.7D, dy, dz * 0.7D);
            }
            if (i % 3 == 0) {
                player.level().addParticle(ParticleTypes.LAVA, x, y - 0.15D, z, dx * 0.2D, 0.02D, dz * 0.2D);
            }
        }
    }

    private static void spawnAntemWindParticles(LocalPlayer player) {
        RandomSource random = player.getRandom();
        double y = player.getY() + 0.8D;

        player.level().addParticle(ParticleTypes.GUST_EMITTER_SMALL, player.getX(), y + 0.4D, player.getZ(), 0.0D, 0.0D, 0.0D);

        for (int i = 0; i < ANTEM_WIND_PARTICLE_COUNT; i++) {
            double angle = (Math.PI * 2.0D * i) / ANTEM_WIND_PARTICLE_COUNT;
            double radius = 0.45D + random.nextDouble() * 1.0D;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double velocityScale = 0.08D + random.nextDouble() * 0.08D;
            double dx = Math.cos(angle) * velocityScale;
            double dz = Math.sin(angle) * velocityScale;
            double dy = 0.02D + random.nextDouble() * 0.04D;

            player.level().addParticle(ParticleTypes.SMALL_GUST, x, y + random.nextDouble() * 0.7D, z, dx, dy, dz);
            if (i % 4 == 0) {
                player.level().addParticle(ParticleTypes.CLOUD, x, y + 0.15D, z, dx * 0.25D, 0.0D, dz * 0.25D);
            }
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

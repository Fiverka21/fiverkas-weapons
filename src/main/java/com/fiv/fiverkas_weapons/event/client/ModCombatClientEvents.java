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
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
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
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_LOGS_CLIENT = Boolean.getBoolean("fweapons.debug");
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
    private static final Set<Integer> SACRILEGIOUS_SLAM_ACTIVE = ConcurrentHashMap.newKeySet();
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
        // Detect local player using Sacrilegious and trigger local client-side animation once
        if (event.getEntity() instanceof LocalPlayer local) {
            boolean usingSac = local.isUsingItem() && local.getUseItem().is(ModItems.SACRILEGIOUS.get());
            int id = local.getId();
            if (usingSac) {
                if (!SACRILEGIOUS_SLAM_ACTIVE.contains(id)) {
                    SACRILEGIOUS_SLAM_ACTIVE.add(id);
                    triggerLocalSacrilegiousSlam(local);
                }
            } else {
                SACRILEGIOUS_SLAM_ACTIVE.remove(id);
            }
        }
    }
    private static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (MKOPI_SHAKE_RENDER_PUSHED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }
    // Called from network when server wants the client to show a sacrilegious slam visual fallback
    public static void handleSacrilegiousSlamClient(int playerId, String animationName) {
        try {
            // Find the player entity in the client world
            if (Minecraft.getInstance().level == null) {
                return;
            }
            var entity = Minecraft.getInstance().level.getEntity(playerId);
            if (!(entity instanceof Player player)) {
                if (DEBUG_LOGS_CLIENT) LOGGER.warn("[fweapons] player entity with id {} is not a Player (found {})", playerId, entity == null ? "null" : entity.getClass().getName());
                return;
            }
            // First attempt: if the player supports BetterCombat's Client-side animatable interface, invoke it directly
            try {
                Class<?> playerAnimatableClass = Class.forName("net.bettercombat.client.animation.PlayerAttackAnimatable");
                if (playerAnimatableClass.isInstance(player)) {
                    Class<?> animatedHandClass = Class.forName("net.bettercombat.logic.AnimatedHand");
                    Object twoHanded = Enum.valueOf((Class<Enum>) animatedHandClass, "TWO_HANDED");
                    Method playMethod = playerAnimatableClass.getMethod("playAttackAnimation", String.class, animatedHandClass, float.class, float.class);
                    // animationName is typically namespaced (e.g. "bettercombat:two_handed_slam")
                    try {
                        playMethod.invoke(player, animationName, twoHanded, 1.625f, 1.2f);
                        if (DEBUG_LOGS_CLIENT) LOGGER.info("[fweapons] PlayerAttackAnimatable.playAttackAnimation invoked on player {}", player.getName().getString());
                    } catch (ReflectiveOperationException e) {
                        if (DEBUG_LOGS_CLIENT) LOGGER.warn("[fweapons] PlayerAttackAnimatable path failed", e);
                    }
                }
            } catch (ReflectiveOperationException e) {
                if (DEBUG_LOGS_CLIENT) LOGGER.warn("[fweapons] PlayerAttackAnimatable path failed", e);
            }
            // Fallback: try to drive BetterCombat client-side to play the actual animation via ClientNetwork packet
            try {
                Class<?> attackAnimClass = Class.forName("net.bettercombat.network.Packets$AttackAnimation");
                Class<?> animatedHandClass = Class.forName("net.bettercombat.logic.AnimatedHand");
                Class<?> clientNetworkClass = Class.forName("net.bettercombat.network.ClientNetwork");
                Object hand = Enum.valueOf((Class<Enum>) animatedHandClass, "MAIN_HAND");
                // length/upswing values chosen to match server-side
                float length = 1.625f;
                float upswing = 1.2f;
                int upswingTicks = Math.round(upswing * 20.0f);
                Object payload = attackAnimClass
                        .getConstructor(
                                int.class,
                                animatedHandClass,
                                String.class,
                                float.class,
                                float.class,
                                float.class,
                                int.class,
                                Class.forName("net.bettercombat.network.Packets$SwingParticles")
                        )
                        .newInstance(player.getId(), hand, animationName, length, upswing, 0.0f, upswingTicks, null);
                // Call client-side handler to play animation locally for this player
                clientNetworkClass.getMethod("handleAttackAnimation", attackAnimClass).invoke(null, payload);
                return;
            } catch (ReflectiveOperationException e) {
                if (DEBUG_LOGS_CLIENT) LOGGER.warn("[fweapons] ClientNetwork.handleAttackAnimation fallback failed", e);
            }
            // Stronger fallback: try to force-play the two_handed_slam keyframe animation using the player-animation library
            try {
                if (DEBUG_LOGS_CLIENT) LOGGER.info("[fweapons] attempting forced player-animation-lib playback for two_handed_slam");
                Class<?> animationCodecsClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs");
                Class<?> keyframeAnimationClass = Class.forName("dev.kosmx.playerAnim.core.data.KeyframeAnimation");
                Class<?> keyframePlayerClass = Class.forName("dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer");
                Class<?> playerAnimationAccessClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess");
                String resourcePath = "assets/fweapons/player_animations/two_handed_slam.json";
                Object keyframeAnim = null;
                java.io.InputStream in = ModCombatClientEvents.class.getClassLoader().getResourceAsStream(resourcePath);
                if (in != null) {
                    try {
                        java.lang.reflect.Method decode = animationCodecsClass.getMethod("deserialize", String.class, java.io.InputStream.class);
                        java.util.Collection<?> parsed = (java.util.Collection<?>) decode.invoke(null, "json", in);
                        if (parsed != null && !parsed.isEmpty()) {
                            keyframeAnim = parsed.iterator().next();
                        }
                    } finally {
                        try { in.close(); } catch (Exception ignored) {}
                    }
                }
                if (keyframeAnim == null) {
                    // try parsing directly with AnimationJson.GSON
                    java.io.InputStream in2 = ModCombatClientEvents.class.getClassLoader().getResourceAsStream(resourcePath);
                    if (in2 != null) {
                        try {
                            Class<?> animationJsonClass = Class.forName("dev.kosmx.playerAnim.core.data.gson.AnimationJson");
                            Object gson = animationJsonClass.getField("GSON").get(null);
                            java.lang.reflect.Method fromJson = gson.getClass().getMethod("fromJson", java.io.Reader.class, Class.class);
                            keyframeAnim = fromJson.invoke(gson, new java.io.InputStreamReader(in2, StandardCharsets.UTF_8), keyframeAnimationClass);
                        } finally {
                            try { in2.close(); } catch (Exception ignored) {}
                        }
                    } else {
                        if (DEBUG_LOGS_CLIENT) LOGGER.warn("[fweapons] could not find resource {} on classpath", resourcePath);
                    }
                }
                if (keyframeAnim != null) {
                    Constructor<?> ctor = keyframePlayerClass.getConstructor(keyframeAnimationClass);
                    Object animPlayer = ctor.newInstance(keyframeAnim);
                    Class<?> abstractClientPlayerClass = Class.forName("net.minecraft.client.player.AbstractClientPlayer");
                    Object animStack = playerAnimationAccessClass.getMethod("getPlayerAnimLayer", abstractClientPlayerClass).invoke(null, player);
                    // add animation layer at high priority
                    boolean addedLayer = false;
                    try {
                        animStack.getClass().getMethod("addAnimLayer", int.class, Class.forName("dev.kosmx.playerAnim.api.layered.IAnimation")).invoke(animStack, 50, animPlayer);
                        addedLayer = true;
                    } catch (NoSuchMethodException nsme) {
                        // try alternative add methods
                        for (Method m : animStack.getClass().getMethods()) {
                            try {
                                String nm = m.getName().toLowerCase();
                                if ((nm.contains("add") || nm.contains("layer")) && m.getParameterCount() >= 1) {
                                    Class<?>[] pts = m.getParameterTypes();
                                    Object[] args = new Object[m.getParameterCount()];
                                    boolean ok = false;
                                    if (m.getParameterCount() == 2 && pts[0] == int.class) {
                                        args[0] = 50;
                                        args[1] = animPlayer;
                                        ok = true;
                                    } else if (m.getParameterCount() == 1) {
                                        args[0] = animPlayer;
                                        ok = true;
                                    }
                                    if (ok) {
                                        m.invoke(animStack, args);
                                        addedLayer = true;
                                        break;
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    } catch (Throwable t) {
                        try {  } catch (Throwable ignored) {}
                    }
                    if (!addedLayer) {
                    }
                    // Try to set first-person mode on animPlayer if possible
                    try {
                        boolean setFP = false;
                        for (Method m : animPlayer.getClass().getMethods()) {
                            String nm = m.getName().toLowerCase();
                            if (nm.contains("first") || nm.contains("perspective") || nm.contains("view")) {
                                Class<?>[] pts = m.getParameterTypes();
                                if (pts.length == 1) {
                                    Class<?> pt = pts[0];
                                    try {
                                        if (pt.isEnum()) {
                                            Object[] consts = pt.getEnumConstants();
                                            if (consts != null && consts.length > 0) {
                                                Object chosen = null;
                                                String[] prefs = new String[]{"FIRST_PERSON","FIRSTPERSON","FIRST","BOTH","ALL","NONE","CLIENT"};
                                                for (Object c : consts) {
                                                    String name = c.toString().toUpperCase();
                                                    for (String p : prefs) {
                                                        if (name.contains(p)) { chosen = c; break; }
                                                    }
                                                    if (chosen != null) break;
                                                }
                                                if (chosen == null) chosen = consts[0];
                                                m.invoke(animPlayer, chosen);
                                                setFP = true;
                                                break;
                                            }
                                        } else if (pt == boolean.class) {
                                            m.invoke(animPlayer, true);
                                            setFP = true;
                                            break;
                                        } else if (pt == int.class) {
                                            m.invoke(animPlayer, 1);
                                            setFP = true;
                                            break;
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }
                        }
                        if (!setFP) {}
                    } catch (Throwable ignored) {}
                    // Try to start/play/restart the animPlayer
                    try {
                        boolean started = false;
                        for (Method m : animPlayer.getClass().getMethods()) {
                            String nm = m.getName().toLowerCase();
                            if (nm.equals("start") || nm.equals("play") || nm.contains("start") || nm.contains("play") || nm.contains("restart") || nm.contains("begin") || nm.contains("reset")) {
                                try {
                                    if (m.getParameterCount() == 0) { m.invoke(animPlayer);  started = true; break; }
                                    else if (m.getParameterCount() == 1) {
                                        Class<?> pt = m.getParameterTypes()[0];
                                        if (pt == float.class || pt == double.class) { m.invoke(animPlayer, 0.0f);  started = true; break; }
                                        else if (pt == int.class) { m.invoke(animPlayer, 0);  started = true; break; }
                                        else if (pt == boolean.class) { m.invoke(animPlayer, true);  started = true; break; }
                                    }
                                } catch (Throwable ignored) {}
                            }
                        }
                        if (!started) {}
                    } catch (Throwable ignored) {}
                    // Try ticking/updating animStack to force immediate frame
                    try {
                        for (Method m : animStack.getClass().getMethods()) {
                            String nm = m.getName().toLowerCase();
                            if (nm.equals("tick") || nm.equals("update") || nm.equals("onupdate") || nm.contains("tick")) {
                                try {
                                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == float.class) { m.invoke(animStack, 1.0f);  }
                                    else if (m.getParameterCount() == 0) { m.invoke(animStack);  }
                                } catch (Throwable ignored) {}
                            }
                        }
                    } catch (Throwable ignored) {}
                    // Diagnostics: isActive/isPlaying
                    try { Method isActive = animPlayer.getClass().getMethod("isActive"); Object val = isActive.invoke(animPlayer);  } catch (NoSuchMethodException ignored) {}
                    try { Method isPlaying = animPlayer.getClass().getMethod("isPlaying"); Object val = isPlaying.invoke(animPlayer);  } catch (NoSuchMethodException ignored) {}
                    // Diagnostic: inspect animStack fields for lists/collections to see active layers
                    try {
                        for (Field f : animStack.getClass().getDeclaredFields()) {
                            f.setAccessible(true);
                            Object v = null;
                            try { v = f.get(animStack); } catch (Throwable ignored) {}
                            if (v instanceof java.util.Collection) {  }
                            else if (v != null) {  }
                            else {  }
                        }
                    } catch (Throwable ignored) {}
                    // Try calling common accessor methods on animStack to enumerate layers
                    try {
                        for (Method m : animStack.getClass().getMethods()) {
                            String name = m.getName().toLowerCase();
                            if (name.contains("layer") || name.contains("layers") || name.contains("active")) {
                                try {
                                    Object res = m.getParameterCount() == 0 ? m.invoke(animStack) : null;
                                    if (res instanceof java.util.Collection) {  }
                                    else if (res != null) {  }
                                } catch (Throwable ignored) {}
                            }
                        }
                    } catch (Throwable ignored) {}
                    if (DEBUG_LOGS_CLIENT) LOGGER.info("[fweapons] forced two_handed_slam animation played for player {}", player.getName().getString());
                    return;
                }
            } catch (Throwable t) {
                if (DEBUG_LOGS_CLIENT) LOGGER.warn("[fweapons] forced player-animation-lib playback failed", t);
            }
            // Spawn some visible particles at the player's position for visual feedback
            Vec3 pos = player.position();
            RandomSource random = Minecraft.getInstance().level.getRandom();
            for (int i = 0; i < 24; i++) {
                double dx = (random.nextDouble() - 0.5) * 0.6;
                double dy = random.nextDouble() * 0.4;
                double dz = (random.nextDouble() - 0.5) * 0.6;
                Minecraft.getInstance().level.addParticle(ParticleTypes.CRIT, pos.x + dx, pos.y + 0.6 + dy, pos.z + dz, dx * 0.2, dy * 0.2, dz * 0.2);
            }
            // Play a sound locally to make the slam feel impactful
            if (Minecraft.getInstance().level != null) {
                Minecraft.getInstance().level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.GENERIC_EXPLODE,
                        net.minecraft.sounds.SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );
            }
            if (DEBUG_LOGS_CLIENT) LOGGER.info("[fweapons] Particle/sound fallback shown for player {}", player.getName().getString());
        } catch (Throwable ignored) {
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
    private static void triggerLocalSacrilegiousSlam(LocalPlayer player) {
        try {
            Class<?> attackAnimClass = Class.forName("net.bettercombat.network.Packets$AttackAnimation");
            Class<?> animatedHandClass = Class.forName("net.bettercombat.logic.AnimatedHand");
            Class<?> clientNetworkClass = Class.forName("net.bettercombat.network.ClientNetwork");
            Class<?> swingParticlesClass = Class.forName("net.bettercombat.network.Packets$SwingParticles");
            Object hand = Enum.valueOf((Class<Enum>) animatedHandClass, "MAIN_HAND");
            float length = 1.625f;
            float upswing = 1.2f;
            int upswingTicks = Math.round(upswing * 20.0f);
            Object particles = swingParticlesClass.getField("EMPTY").get(null);
            // Stop the local use action so BetterCombat's attack animation can play instead of the generic use animation.
            try {
                player.stopUsingItem();
            } catch (Throwable ignoredStop) {
            }
            Object payload = attackAnimClass
                    .getConstructor(
                            int.class,
                            animatedHandClass,
                            String.class,
                            float.class,
                            float.class,
                            float.class,
                            int.class,
                            swingParticlesClass
                    )
                    .newInstance(
                            player.getId(),
                            hand,
                            "bettercombat:two_handed_slam",
                            length,
                            upswing,
                            0.0f,
                            upswingTicks,
                            particles
                    );
            try {
                clientNetworkClass.getMethod("handleAttackAnimation", attackAnimClass).invoke(null, payload);
                if (DEBUG_LOGS_CLIENT) LOGGER.info("[fweapons] clientNetwork handleAttackAnimation invoked locally for player {}", player.getName().getString());
            } catch (ReflectiveOperationException e) {
                // If reflection fails, do nothing; server fallback will show particles.
            }
            // New: attempt to send a real BetterCombat C2S_AttackRequest from the client so the server
            // creates the proper internal attack state (preferred over server-side reflective invocation).
            try {
                Class<?> attackReqClass = Class.forName("net.bettercombat.network.Packets$C2S_AttackRequest");
                Constructor<?> attackCtor = null;
                try {
                    attackCtor = attackReqClass.getConstructor(int.class, boolean.class, int.class, int.class, int[].class);
                } catch (NoSuchMethodException nsme) {
                    for (Constructor<?> c : attackReqClass.getConstructors()) {
                        Class<?>[] pts = c.getParameterTypes();
                        if (pts.length >= 1 && pts[0] == int.class) {
                            attackCtor = c;
                            break;
                        }
                    }
                }
                if (attackCtor != null) {
                    int lastIndex = 5;
                    int selectedSlot = 0;
                    try { selectedSlot = player.getInventory().selected; } catch (Throwable ignored) {}
                    Object attackReq;
                    if (attackCtor.getParameterCount() == 5) {
                        attackReq = attackCtor.newInstance(lastIndex, false, selectedSlot, 0, new int[0]);
                    } else {
                        Object[] args = new Object[attackCtor.getParameterCount()];
                        for (int i = 0; i < args.length; i++) args[i] = 0;
                        args[0] = lastIndex;
                        attackReq = attackCtor.newInstance((Object[]) args);
                    }
                    Object clientConn = null;
                    try {
                        clientConn = Minecraft.getInstance().getConnection();
                    } catch (Throwable ignored) {}
                    if (clientConn == null) {
                        try {
                            Field connField = player.getClass().getDeclaredField("connection");
                            connField.setAccessible(true);
                            clientConn = connField.get(player);
                        } catch (Throwable ignored) {}
                    }
                    if (clientConn != null) {
                        try {
                            Method sendMethod = clientConn.getClass().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
                            sendMethod.invoke(clientConn, attackReq);
                        } catch (NoSuchMethodException nsme) {
                            try {
                                Method sendAny = clientConn.getClass().getMethod("send", Object.class);
                                sendAny.invoke(clientConn, attackReq);
                            } catch (Throwable t) {
                            }
                        }
                    } else {
                        System.out.println("[fweapons] client connection not found; cannot send C2S_AttackRequest");
                    }
                } else {
                }
            } catch (ReflectiveOperationException e) {
            }
        // As a stronger fallback, try to invoke BetterCombat's client 'attack start' publisher so the
        // animation runs within the mod's normal attack flow. Try to construct an attack-hand object
        // and call any publisher methods reflectively.
        try {
            Class<?> eventsClass = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents");
            Object attackStartPublisher = eventsClass.getField("ATTACK_START").get(null);
            Method[] methods = attackStartPublisher.getClass().getMethods();
            for (Method m : methods) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length < 2) continue;
                // require first parameter to be assignable from LocalPlayer (or Player)
                try {
                    Class<?> localPlayerClass = Class.forName("net.minecraft.client.player.LocalPlayer");
                    if (!params[0].isAssignableFrom(localPlayerClass)) {
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    // no local player class found for some reason; skip
                    continue;
                }
                Class<?> attackHandType = params[1];
                Object attackHandObj = null;
                // Try to create a proxy instance if the attack hand type is an interface
                if (attackHandType.isInterface()) {
                    // try to find an 'attack' nested type by inspecting the methods of attackHandType
                    // create a proxy for the nested attack object if the attack type is an interface
                    try {
                        // Build attack proxy implementing any interface methods called 'hitbox'/'animation'
                        InvocationHandler attackHandler = (proxy, method, args) -> {
                        String name = method.getName();
                        if ("hitbox".equals(name) || "getHitbox".equals(name)) return "VERTICAL_PLANE";
                        if ("animation".equals(name) || "getAnimation".equals(name)) return "bettercombat:two_handed_slam";
                        if (method.getReturnType().isPrimitive()) {
                            if (method.getReturnType() == boolean.class) return false;
                            if (method.getReturnType() == int.class) return 0;
                            if (method.getReturnType() == long.class) return 0L;
                            if (method.getReturnType() == float.class) return 0.0f;
                            if (method.getReturnType() == double.class) return 0.0d;
                        }
                        return null;
                    };
                    // Build attack-hand proxy implementing attackHandType
                    InvocationHandler attackHandHandler = (proxy, method, args) -> {
                        String name = method.getName();
                        if ("itemStack".equals(name) || "getItemStack".equals(name) || "getItem".equals(name)) {
                            return player.getMainHandItem();
                        }
                        if ("getHitbox".equals(name) || "hitbox".equals(name) || "getHitboxName".equals(name)) {
                            return "VERTICAL_PLANE";
                        }
                        if ("getAnimation".equals(name) || "animation".equals(name) || "getAnimationName".equals(name)) {
                            return "bettercombat:two_handed_slam";
                        }
                        if ("attack".equals(name) || "getAttack".equals(name)) {
                            // try to create an attack proxy on demand using the declared return type of the attack method
                            try {
                                for (Method am : attackHandType.getMethods()) {
                                    if (am.getName().equals("attack") || am.getName().equals("getAttack")) {
                                        Class<?> attackReturnType = am.getReturnType();
                                        if (attackReturnType.isInterface()) {
                                            Object attackObjProxy = java.lang.reflect.Proxy.newProxyInstance(
                                                    attackReturnType.getClassLoader(),
                                                    new Class<?>[]{attackReturnType},
                                                    attackHandler
                                            );
                                            return attackObjProxy;
                                        }
                                        break;
                                    }
                                }
                            } catch (Throwable ignored) {
                            }
                            return null;
                        }
                        if (method.getReturnType().isPrimitive()) {
                            if (method.getReturnType() == boolean.class) return false;
                            if (method.getReturnType() == int.class) return 0;
                            if (method.getReturnType() == long.class) return 0L;
                            if (method.getReturnType() == float.class) return 0.0f;
                            if (method.getReturnType() == double.class) return 0.0d;
                        }
                        return null;
                    };
                    attackHandObj = java.lang.reflect.Proxy.newProxyInstance(
                            attackHandType.getClassLoader(),
                            new Class<?>[]{attackHandType},
                            attackHandHandler
                    );
                    } catch (Throwable ignored) {
                    }
                }
                // Attempt to invoke publisher method with (player, attackHandObj) if we constructed one
                if (attackHandObj != null) {
                    try {
                        // Try to set player's current attack state so BetterCombat treats this as a real attack
                        try {
                            Method setCurrent = null;
                            try {
                                setCurrent = player.getClass().getMethod("setCurrentAttack", attackHandObj.getClass());
                            } catch (NoSuchMethodException ignoredSet) {
                                // Try searching for any setCurrentAttack with any param type
                                for (Method candidate : player.getClass().getMethods()) {
                                    if (candidate.getName().equals("setCurrentAttack") && candidate.getParameterCount() == 1) {
                                        setCurrent = candidate;
                                        break;
                                    }
                                }
                            }
                            if (setCurrent != null) {
                                setCurrent.setAccessible(true);
                                setCurrent.invoke(player, attackHandObj);
                            } else {
                                // Try a field named currentAttack
                                try {
                                    Field currentAttackField = player.getClass().getDeclaredField("currentAttack");
                                    currentAttackField.setAccessible(true);
                                    currentAttackField.set(player, attackHandObj);
                                } catch (NoSuchFieldException ignoredField) {
                                    // ignore
                                }
                            }
                        } catch (Throwable ignoreSetCur) {
                        }
                        m.invoke(attackStartPublisher, player, attackHandObj);
                        return; // success
                    } catch (ReflectiveOperationException | IllegalArgumentException ex) {
                        // continue trying other methods
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            if (DEBUG_LOGS_CLIENT) LOGGER.warn("[fweapons] attackStart publisher invocation failed", e);
            // ignore
        }
        } catch (Throwable t) {
        }
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

package com.fiv.fiverkas_weapons.effect;

import com.fiv.fiverkas_weapons.FiverkasWeapons;
import com.fiv.fiverkas_weapons.util.CompatIds;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SunsetEffect extends MobEffect {
    private static final int SUNSET_COLOR = 0xFFFFB347;
    private static final double SLOW_PER_STACK = -0.1D;
    private static final int MAX_SLOW_STACKS = 8;
    private static final float DAMAGE_PER_SECOND_PER_STACK = 0.25F;
    private static final String INT2DOUBLE_FUNCTION_CLASS = "it.unimi.dsi.fastutil.ints.Int2DoubleFunction";
    private static final ResourceKey<DamageType> SUNSET_DAMAGE = CompatIds.resourceKey(
            Registries.DAMAGE_TYPE,
            FiverkasWeapons.MODID,
            "sunset"
    );

    public SunsetEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFB347);
        addSunsetSlowModifier();
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.tickCount % 20 == 0) {
            int stacks = amplifier + 1;
            float damage = DAMAGE_PER_SECOND_PER_STACK * stacks;
            DamageSource sunsetDamage = new DamageSource(
                    level.registryAccess()
                            .lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(SUNSET_DAMAGE)
            );
            entity.hurt(sunsetDamage, damage);
        }
        // Returning false removes the effect instance in 1.21.1; keep it active.
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public ParticleOptions createParticleOptions(MobEffectInstance effect) {
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, SUNSET_COLOR);
    }

    private void addSunsetSlowModifier() {
        Object id = CompatIds.id(FiverkasWeapons.MODID, "sunset_slow");
        try {
            Method method = findAddAttributeModifier(id);
            if (method == null) {
                return;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 4 && params[2] == AttributeModifier.Operation.class && isInt2DoubleFunction(params[3])) {
                method.invoke(
                        this,
                        Attributes.MOVEMENT_SPEED,
                        id,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                        createSunsetCurve()
                );
            } else {
                method.invoke(
                        this,
                        Attributes.MOVEMENT_SPEED,
                        id,
                        SLOW_PER_STACK,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                );
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to add Sunset slow modifier", e);
        }
    }

    private static Method findAddAttributeModifier(Object id) {
        Class<?> idClass = id.getClass();
        for (Method method : MobEffect.class.getMethods()) {
            if (!method.getName().equals("addAttributeModifier") || method.getParameterCount() != 4) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (!params[1].isAssignableFrom(idClass) && !idClass.isAssignableFrom(params[1])) {
                continue;
            }
            return method;
        }
        return null;
    }

    private static boolean isInt2DoubleFunction(Class<?> type) {
        return INT2DOUBLE_FUNCTION_CLASS.equals(type.getName());
    }

    private static Object createSunsetCurve() {
        try {
            Class<?> funcType = Class.forName(INT2DOUBLE_FUNCTION_CLASS);
            return Proxy.newProxyInstance(
                    SunsetEffect.class.getClassLoader(),
                    new Class<?>[]{funcType},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(proxy, args);
                        }
                        Class<?> returnType = method.getReturnType();
                        if (returnType != double.class && returnType != Double.class) {
                            if (returnType == boolean.class) {
                                return false;
                            }
                            if (returnType == int.class) {
                                return 0;
                            }
                            return null;
                        }
                        int level = 0;
                        if (args != null && args.length > 0 && args[0] instanceof Number number) {
                            level = number.intValue();
                        }
                        double amount = SLOW_PER_STACK * Math.min(level + 1, MAX_SLOW_STACKS);
                        return returnType == double.class ? amount : Double.valueOf(amount);
                    }
            );
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Int2DoubleFunction class not found", e);
        }
    }
}

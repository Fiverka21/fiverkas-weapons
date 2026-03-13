package com.fiv.fiverkas_weapons.util;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Compatibility helpers for ResourceLocation (1.21.10) vs Identifier (1.21.11+).
 * Keeps the main code free of direct references to either class.
 */
public final class CompatIds {
    private static final Class<?> ID_CLASS;
    private static final Method ID_FROM_NAMESPACE;
    private static final Constructor<?> ID_CTOR_2;
    private static final Constructor<?> ID_CTOR_1;
    private static final Method RESOURCE_KEY_CREATE;
    private static final Constructor<?> PAYLOAD_TYPE_CTOR;
    private static final Method SOUND_EVENT_CREATE;
    private static final Method MOB_EFFECT_ADD_ATTRIBUTE;
    private static final boolean MOB_EFFECT_ADD_ATTRIBUTE_USES_FUNCTION;
    private static final Method ITEM_PROPERTIES_SET_ID;
    private static final String INT2DOUBLE_FUNCTION_CLASS = "it.unimi.dsi.fastutil.ints.Int2DoubleFunction";

    static {
        Class<?> idClass;
        Method fromNamespace = null;
        Constructor<?> ctor2 = null;
        Constructor<?> ctor1 = null;
        try {
            idClass = Class.forName("net.minecraft.resources.Identifier");
        } catch (ClassNotFoundException e) {
            try {
                idClass = Class.forName("net.minecraft.resources.ResourceLocation");
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("No identifier class found", ex);
            }
        }

        try {
            fromNamespace = idClass.getMethod("fromNamespaceAndPath", String.class, String.class);
        } catch (NoSuchMethodException ignored) {
        }

        try {
            ctor2 = idClass.getConstructor(String.class, String.class);
        } catch (NoSuchMethodException ignored) {
        }
        try {
            ctor1 = idClass.getConstructor(String.class);
        } catch (NoSuchMethodException ignored) {
        }
        if (fromNamespace == null && ctor2 == null && ctor1 == null) {
            throw new IllegalStateException("No identifier constructor found");
        }

        ID_CLASS = idClass;
        ID_FROM_NAMESPACE = fromNamespace;
        ID_CTOR_2 = ctor2;
        ID_CTOR_1 = ctor1;
        RESOURCE_KEY_CREATE = findStaticMethod(ResourceKey.class, "create", 2);
        PAYLOAD_TYPE_CTOR = findConstructor(CustomPacketPayload.Type.class, 1);
        SOUND_EVENT_CREATE = findStaticMethod(SoundEvent.class, "createVariableRangeEvent", 1);
        MobEffectAddAttributeResult addAttributeResult = findMobEffectAddAttribute(idClass);
        MOB_EFFECT_ADD_ATTRIBUTE = addAttributeResult.method;
        MOB_EFFECT_ADD_ATTRIBUTE_USES_FUNCTION = addAttributeResult.usesFunction;
        ITEM_PROPERTIES_SET_ID = findItemPropertiesSetId();
    }

    private CompatIds() {
    }

    public static Object id(String namespace, String path) {
        try {
            if (ID_FROM_NAMESPACE != null) {
                return ID_FROM_NAMESPACE.invoke(null, namespace, path);
            }
            if (ID_CTOR_2 != null) {
                return ID_CTOR_2.newInstance(namespace, path);
            }
            return ID_CTOR_1.newInstance(namespace + ":" + path);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to build identifier", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ResourceKey<T> resourceKey(ResourceKey<?> registry, String namespace, String path) {
        Object id = id(namespace, path);
        try {
            return (ResourceKey<T>) RESOURCE_KEY_CREATE.invoke(null, registry, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create ResourceKey", e);
        }
    }

    public static SoundEvent soundEvent(String namespace, String path) {
        Object id = id(namespace, path);
        try {
            return (SoundEvent) SOUND_EVENT_CREATE.invoke(null, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create SoundEvent", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String namespace, String path) {
        Object id = id(namespace, path);
        try {
            return (CustomPacketPayload.Type<T>) PAYLOAD_TYPE_CTOR.newInstance(id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create payload type", e);
        }
    }

    public static void addAttributeModifier(MobEffect effect, Object attribute, String namespace, String path, double amount, AttributeModifier.Operation operation) {
        Object id = id(namespace, path);
        try {
            if (MOB_EFFECT_ADD_ATTRIBUTE_USES_FUNCTION) {
                Object function = constantInt2Double(amount);
                MOB_EFFECT_ADD_ATTRIBUTE.invoke(effect, attribute, id, operation, function);
            } else {
                MOB_EFFECT_ADD_ATTRIBUTE.invoke(effect, attribute, id, amount, operation);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to add attribute modifier", e);
        }
    }

    public static Item.Properties setItemId(Item.Properties properties, ResourceKey<?> registry, String namespace, String path) {
        ResourceKey<?> key = resourceKey(registry, namespace, path);
        try {
            ITEM_PROPERTIES_SET_ID.invoke(properties, key);
            return properties;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set Item.Properties id", e);
        }
    }

    private static MobEffectAddAttributeResult findMobEffectAddAttribute(Class<?> idClass) {
        Method fallback = null;
        for (Method method : MobEffect.class.getMethods()) {
            if (!method.getName().equals("addAttributeModifier") || method.getParameterCount() != 4) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (!idParamMatches(params[1], idClass)) {
                continue;
            }
            if (isDoubleLike(params[2]) && AttributeModifier.Operation.class.isAssignableFrom(params[3])) {
                return new MobEffectAddAttributeResult(method, false);
            }
            if (AttributeModifier.Operation.class.isAssignableFrom(params[2]) && isInt2DoubleFunction(params[3])) {
                return new MobEffectAddAttributeResult(method, true);
            }
            if (fallback == null) {
                fallback = method;
            }
        }
        if (fallback != null) {
            return new MobEffectAddAttributeResult(fallback, false);
        }
        throw new IllegalStateException("Method addAttributeModifier not found on " + MobEffect.class.getName());
    }

    private static Method findStaticMethod(Class<?> owner, String name, int paramCount) {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        throw new IllegalStateException("Method " + name + " not found on " + owner.getName());
    }

    private static Method findMethod(Class<?> owner, String name, int paramCount) {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) {
                return method;
            }
        }
        throw new IllegalStateException("Method " + name + " not found on " + owner.getName());
    }

    private static Constructor<?> findConstructor(Class<?> owner, int paramCount) {
        for (Constructor<?> ctor : owner.getConstructors()) {
            if (ctor.getParameterCount() == paramCount) {
                return ctor;
            }
        }
        throw new IllegalStateException("Constructor with " + paramCount + " args not found on " + owner.getName());
    }

    private static Method findItemPropertiesSetId() {
        Method fallback = null;
        for (Method method : Item.Properties.class.getMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            if (!ResourceKey.class.isAssignableFrom(method.getParameterTypes()[0])) {
                continue;
            }
            if (!Item.Properties.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            if ("setId".equals(method.getName())) {
                return method;
            }
            if (fallback == null) {
                fallback = method;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new IllegalStateException("Item.Properties id setter not found");
    }

    private static boolean idParamMatches(Class<?> parameterType, Class<?> idClass) {
        return parameterType.isAssignableFrom(idClass) || idClass.isAssignableFrom(parameterType);
    }

    private static boolean isDoubleLike(Class<?> type) {
        return type == double.class || type == Double.class;
    }

    private static boolean isInt2DoubleFunction(Class<?> type) {
        return INT2DOUBLE_FUNCTION_CLASS.equals(type.getName());
    }

    private static Object constantInt2Double(double value) {
        try {
            Class<?> funcType = Class.forName(INT2DOUBLE_FUNCTION_CLASS);
            return Proxy.newProxyInstance(
                    CompatIds.class.getClassLoader(),
                    new Class<?>[]{funcType},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(proxy, args);
                        }
                        String name = method.getName();
                        Class<?> returnType = method.getReturnType();
                        if (returnType == double.class) {
                            return value;
                        }
                        if (returnType.isAssignableFrom(Double.class)) {
                            return Double.valueOf(value);
                        }
                        if (returnType == Object.class) {
                            if ("apply".equals(name)
                                    || "get".equals(name)
                                    || "getOrDefault".equals(name)
                                    || "put".equals(name)
                                    || "remove".equals(name)) {
                                return Double.valueOf(value);
                            }
                        }
                        if (returnType == boolean.class) {
                            return false;
                        }
                        if (returnType == int.class) {
                            return 0;
                        }
                        return null;
                    }
            );
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Int2DoubleFunction class not found", e);
        }
    }

    private static final class MobEffectAddAttributeResult {
        private final Method method;
        private final boolean usesFunction;

        private MobEffectAddAttributeResult(Method method, boolean usesFunction) {
            this.method = method;
            this.usesFunction = usesFunction;
        }
    }
}

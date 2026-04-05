package net.neoforged.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DeferredRegister<T> {
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;
    private final List<Pending<T, ? extends T>> pending = new ArrayList<>();

    private DeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new DeferredRegister<>(registryKey, namespace);
    }

    public <I extends T> DeferredHolder<T, I> register(String name, Supplier<I> supplier) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, name);
        DeferredHolder<T, I> holder = new DeferredHolder<>(id);
        pending.add(new Pending<>(holder, supplier));
        return holder;
    }

    public void register(IEventBus ignored) {
        Registry<T> registry = resolveRegistry();
        for (Pending<T, ? extends T> registration : pending) {
            register(registry, registration);
        }
    }

    @SuppressWarnings("unchecked")
    private Registry<T> resolveRegistry() {
        if (registryKey.equals(Registries.ITEM)) {
            return (Registry<T>) BuiltInRegistries.ITEM;
        }
        if (registryKey.equals(Registries.MOB_EFFECT)) {
            return (Registry<T>) BuiltInRegistries.MOB_EFFECT;
        }
        if (registryKey.equals(Registries.CREATIVE_MODE_TAB)) {
            return (Registry<T>) BuiltInRegistries.CREATIVE_MODE_TAB;
        }
        if (registryKey.equals(Registries.SOUND_EVENT)) {
            return (Registry<T>) BuiltInRegistries.SOUND_EVENT;
        }
        throw new IllegalStateException("Unsupported deferred registry key: " + registryKey.location());
    }

    @SuppressWarnings("unchecked")
    private <I extends T> void register(Registry<T> registry, Pending<T, I> registration) {
        I value = registration.factory().get();
        Registry.register(registry, registration.holder().getId(), value);
        ((DeferredHolder<T, I>) registration.holder()).set(value);
    }

    private record Pending<R, T extends R>(DeferredHolder<R, T> holder, Supplier<T> factory) {
    }
}

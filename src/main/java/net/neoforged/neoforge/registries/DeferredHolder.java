package net.neoforged.neoforge.registries;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class DeferredHolder<R, T extends R> implements Supplier<T> {
    private final ResourceLocation id;
    private T value;

    DeferredHolder(ResourceLocation id) {
        this.id = id;
    }

    void set(T value) {
        this.value = value;
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public T get() {
        if (value == null) {
            throw new IllegalStateException("Deferred value has not been registered yet: " + id);
        }
        return value;
    }
}

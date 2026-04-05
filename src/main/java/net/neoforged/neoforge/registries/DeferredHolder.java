package net.neoforged.neoforge.registries;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DeferredHolder<R, T extends R> implements Supplier<T>, Holder<R> {
    private final Identifier id;
    private final ResourceKey<R> key;
    private T value;

    DeferredHolder(ResourceKey<R> key, Identifier id) {
        this.key = key;
        this.id = id;
    }

    void set(T value) {
        this.value = value;
    }

    public Identifier getId() {
        return id;
    }

    @Override
    public T get() {
        if (value == null) {
            throw new IllegalStateException("Deferred value has not been registered yet: " + id);
        }
        return value;
    }

    @Override
    public R value() {
        return get();
    }

    @Override
    public boolean isBound() {
        return value != null;
    }

    @Override
    public boolean is(Identifier id) {
        return this.id.equals(id);
    }

    @Override
    public boolean is(ResourceKey<R> resourceKey) {
        return key.equals(resourceKey);
    }

    @Override
    public boolean is(Predicate<ResourceKey<R>> predicate) {
        return predicate.test(key);
    }

    @Override
    public boolean is(TagKey<R> tagKey) {
        return false;
    }

    @Override
    public boolean is(Holder<R> holder) {
        return this == holder || (isBound() && holder.isBound() && value().equals(holder.value()));
    }

    @Override
    public Stream<TagKey<R>> tags() {
        return Stream.empty();
    }

    @Override
    public Either<ResourceKey<R>, R> unwrap() {
        return isBound() ? Either.right(value()) : Either.left(key);
    }

    @Override
    public Optional<ResourceKey<R>> unwrapKey() {
        return Optional.of(key);
    }

    @Override
    public Kind kind() {
        return Kind.REFERENCE;
    }

    @Override
    public boolean canSerializeIn(HolderOwner<R> owner) {
        return true;
    }
}

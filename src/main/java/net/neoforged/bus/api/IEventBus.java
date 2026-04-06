package net.neoforged.bus.api;

import java.util.function.Consumer;

public interface IEventBus {
    <T> void addListener(Class<T> eventType, Consumer<? super T> listener);
}

package net.neoforged.bus.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SimpleEventBus implements IEventBus {
    private final List<Consumer<Object>> listeners = new CopyOnWriteArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> void addListener(Consumer<T> listener) {
        listeners.add((Consumer<Object>) listener);
    }

    public void post(Object event) {
        for (Consumer<Object> listener : listeners) {
            try {
                listener.accept(event);
            } catch (ClassCastException ignored) {
            }
        }
    }
}

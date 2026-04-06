package net.neoforged.bus.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SimpleEventBus implements IEventBus {
    private final List<Listener<?>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public <T> void addListener(Class<T> eventType, Consumer<? super T> listener) {
        listeners.add(new Listener<>(eventType, listener));
    }

    public void post(Object event) {
        for (Listener<?> listener : listeners) {
            listener.dispatch(event);
        }
    }

    private record Listener<T>(Class<T> eventType, Consumer<? super T> listener) {
        private void dispatch(Object event) {
            if (eventType.isInstance(event)) {
                listener.accept(eventType.cast(event));
            }
        }
    }
}

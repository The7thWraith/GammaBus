package dev.spectre.gammabus.bus;

import dev.spectre.gammabus.Event;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

public class LinkWrapper<T extends Event> {
    private final Link<T> link;
    @Getter
    private final EventPriority priority;
    @Getter
    private final Object parent;
    @Getter
    private final Class<?> type;

    @Getter
    private final AsyncPoolType asyncPoolType;

    public LinkWrapper(Object parent, Field field) {
        this.priority = field.getAnnotation(Subscriber.class).value();
        this.parent = parent;
        this.type = ((Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
        boolean isAsync = field.isAnnotationPresent(Async.class);
        Link<T> link = null;
        try {
            link = (Link<T>) field.get(parent);
        } catch (IllegalAccessException e) { e.printStackTrace(); }
        this.link = link;

        Async asyncAnnotation = field.getAnnotation(Async.class);
        this.asyncPoolType = asyncAnnotation != null ? asyncAnnotation.value() : null;
    }

    public void fire(Event event) {
        this.link.call((T) event);
    }

    public boolean isAsync() {
        return asyncPoolType != null;
    }
}
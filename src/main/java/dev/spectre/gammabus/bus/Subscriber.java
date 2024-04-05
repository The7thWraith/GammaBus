package dev.spectre.gammabus.bus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscriber {

    /**
     * The priority of the event.
     * @return The priority of the event.
     */
    EventPriority value() default EventPriority.DEFAULT;
}

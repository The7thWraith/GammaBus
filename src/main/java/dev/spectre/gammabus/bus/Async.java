package dev.spectre.gammabus.bus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by The7thWraith on 1/6/2024
 *
 * @author The7thWraith
 * @date 1/6/2024
 * @project GammaBus
 */


/**
    * This annotation is used to mark a subscriber to be executed
    * asynchronously via the EventBus's thread pool executor.
    * <p>
    * The AsyncPoolType value is used to determine which thread pool type
    * the subscriber will be submitted to. Either the fixed thread pool (more efficient)
    * or the cached thread pool (less efficient).
    * <p>
    * Asynchronous execution of event handlers is, in general, rarely necessary.
 */

@Target(ElementType.FIELD)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface Async {
    AsyncPoolType value();
}

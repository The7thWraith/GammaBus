package dev.spectre.gammabus.bus;

import dev.spectre.gammabus.Event;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.concurrent.*;

public final class EventBus {

    
    private final CopyOnWriteArrayList<LinkWrapper<? extends Event>> eventlinks = new CopyOnWriteArrayList<>();
    private final ExecutorService cachedExecutorService = Executors.newCachedThreadPool();
    private final ExecutorService fixedExecutorService;


    @Getter
    private final BlockingQueue<Runnable> asyncCachedEventQueue = new LinkedBlockingQueue<>();
    @Getter
    private final BlockingQueue<Runnable> asyncFixedEventQueue = new LinkedBlockingQueue<>();

    private final Thread asyncCachedEventProcessorThread;
    private final Thread asyncFixedEventProcessorThread;

    private final boolean threadedManagement;


    public EventBus(int fixedThreadPoolSize, boolean threadedManagement) {
        fixedExecutorService = Executors.newFixedThreadPool(fixedThreadPoolSize);
        asyncCachedEventProcessorThread = createProcessorThread(asyncCachedEventQueue, cachedExecutorService);
        asyncFixedEventProcessorThread = createProcessorThread(asyncFixedEventQueue, fixedExecutorService);
        if(threadedManagement) {
            asyncCachedEventProcessorThread.start();
            asyncFixedEventProcessorThread.start();
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        }
        this.threadedManagement = threadedManagement;
    }


    // TODO: Make it optional to go through all non-async events and fire them first.
    public void fire(Event event) {
        for (LinkWrapper<? extends Event> listenerWrapper : eventlinks) {
            if (event.getClass() != listenerWrapper.getType())
                continue;

            if (listenerWrapper.isAsync()) {
                if(listenerWrapper.getAsyncPoolType() == AsyncPoolType.CACHED)  {
                    /**
                     *  This is the slowest method of executing events,
                     *  taking around 1400ms for 1,000,000 event calls.
                     *  Performance is highly variable, and will most definitely
                     *  decrease significantly as the number of threads increases.
                     *  It is also the most memory-intensive.
                     **/
                    if(threadedManagement) {
                        asyncCachedEventQueue.offer(() -> listenerWrapper.fire(event));
                    } else {
                        cachedExecutorService.submit(() -> listenerWrapper.fire(event));
                    }
                }
                else if(listenerWrapper.getAsyncPoolType() == AsyncPoolType.FIXED) {
                    /**
                     * This is the second fastest method of executing events
                     * It is still between one and two orders of magnitude slower
                     * than synchronous execution (80-800ms for 1,000,000 event calls)
                     **/
                    if(threadedManagement) {
                        asyncFixedEventQueue.offer(() -> listenerWrapper.fire(event));
                    } else {
                        fixedExecutorService.submit(() -> listenerWrapper.fire(event));
                    }
                }
            } else {
                /**
                 * Execute synchronously on the main thread
                 * This is the fastest way to execute events
                 * 2-4ms for 1,000,000 event calls
                 **/
                listenerWrapper.fire(event);
            }
        }
    }


    /**
     * Subscribes a class to the eventbus.
     *
     * @param object The class to subscribe.
     */
    public void subscribe(Object object) {
        if (isSubscribed(object)) return;
        for (Field field : object.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Subscriber.class) || !field.getType().isAssignableFrom(Link.class))
                continue;
            if (!field.isAccessible()) field.setAccessible(true);
            eventlinks.add(new LinkWrapper<>(object, field));
        }
        eventlinks.sort(Comparator.comparingInt(listener -> listener.getPriority().ordinal()));
    }

    /**
     * Unsubscribes a class.
     *
     * @param object The class to unsubscribe.
     */
    public void unsubscribe(Object object) {
        eventlinks.removeIf(listener -> listener.getParent().getClass().equals(object.getClass()));
    }

    /**
     * Kills async threads and their corresponding executor service
     * if there are @Async-annotated fields in registered classes
     * with the corresponding AsyncPoolType value.
     */
    public void cullAsync() {
        boolean hasCachedListeners = eventlinks.stream()
                .anyMatch(listener -> listener.isAsync() && listener.getAsyncPoolType() == AsyncPoolType.CACHED);
        boolean hasFixedListeners = eventlinks.stream()
                .anyMatch(listener -> listener.isAsync() && listener.getAsyncPoolType() == AsyncPoolType.FIXED);

        if (!hasCachedListeners && asyncCachedEventProcessorThread.isAlive()) {
            asyncCachedEventProcessorThread.interrupt();
            killExecutor(cachedExecutorService);
        }
        if (!hasFixedListeners && asyncFixedEventProcessorThread.isAlive()) {
            asyncFixedEventProcessorThread.interrupt();
            killExecutor(fixedExecutorService);
        }
    }

    /**
     * Kills an executor service.
     * @param executorService The executor service to kill.
     */
    private void killExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if a class is subscribed to the event bus.
     *
     * @param object The class to check.
     * @return True/False of whether or not the class is
     * subscribed.
     */
    public boolean isSubscribed(Object object) {
        return eventlinks.stream().anyMatch(listener -> listener.getParent().getClass().equals(object.getClass()));
    }

    /**
     * Shutdown the eventbus, and kill all threads
     * and executor services.
     */
    public void shutdown() {
        asyncCachedEventProcessorThread.interrupt();
        asyncFixedEventProcessorThread.interrupt();

        cachedExecutorService.shutdown();
        fixedExecutorService.shutdown();
        try {
            if (!cachedExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                cachedExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            cachedExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates a processor thread to manage asynchronous events
     *
     * @param queue the queue that the thread will take from
     * @param service the service that the thread will submit to
     * @return Thread
     */
    private Thread createProcessorThread(BlockingQueue<Runnable> queue, ExecutorService service) {
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    service.submit(queue.take());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.setDaemon(true);
        return thread;
    }
}
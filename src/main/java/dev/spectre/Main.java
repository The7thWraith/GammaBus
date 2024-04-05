package dev.spectre;

import dev.spectre.gammabus.bus.*;
import dev.spectre.gammabus.events.TestEvent;

public class Main {

    public static void main(String[] args) {
        EventBus eventBus = new EventBus(3, false);
        eventBus.subscribe(new Main()); // Subscribe a class -- all annotated fields in the class will be subscribed
        eventBus.cullAsync(); // Cull the threadpools and their respective threads if they are not used
        TestEvent event = new TestEvent("Hello, World!");
        eventBus.fire(event); // Fire an event

        // Async events might fire at different times or get weird updates.
        // Make sure to watch out for those!
        System.out.println("The new message is " + event.getMessage());
        System.out.println("\n");
        eventBus.shutdown();
    }

    @Subscriber
    private final Link<TestEvent> testEventLink = event ->  {
        System.out.println("I'm on the main thread!");
        System.out.println("TestEvent received on the main thread with message: " + event.getMessage());
        System.out.println("\n");
    };

    @Async(AsyncPoolType.FIXED)
    @Subscriber
    private final Link<TestEvent> asyncTestEventLink = event ->  {
        System.out.println("I'm on a different thread!");
        System.out.println("TestEvent received on separate thread with message: " + event.getMessage());
        System.out.println("\n");
    };

    @Subscriber(EventPriority.HIGHEST)
    private final Link<TestEvent> highPriorityTestEventLink = event ->  {
        System.out.println("I'm more important!");
        System.out.println("TestEvent received on high priority handler with message: " + event.getMessage());
        System.out.println("\n");

        event.setMessage("Hello, Universe!"); // Changing the message will immediately reflect in the event object
    };
}
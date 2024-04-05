package dev.spectre.gammabus.events;


import dev.spectre.gammabus.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicReference;

public class TestEvent extends Event {
    private final AtomicReference<String> message;

    public TestEvent(String message) {
        this.message = new AtomicReference<>(message);
    }

    public String getMessage() {
        return message.get();
    }

    public void setMessage(String message) {
        this.message.set(message);
    }
}

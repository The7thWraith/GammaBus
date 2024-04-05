package dev.spectre.gammabus;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;

@Setter
@Getter
public abstract class Event {
    protected final AtomicBoolean canceled = new AtomicBoolean(false);
    protected EventState state;

    public void setCanceled(boolean canceled) {
        this.canceled.set(canceled);
    }

    public boolean isCanceled() {
        return this.canceled.get();
    }
}
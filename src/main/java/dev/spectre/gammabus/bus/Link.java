package dev.spectre.gammabus.bus;


import dev.spectre.gammabus.Event;

public interface Link<T extends Event> {
    void call(T event);
}
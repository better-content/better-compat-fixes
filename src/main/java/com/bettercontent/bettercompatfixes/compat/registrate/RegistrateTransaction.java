package com.bettercontent.bettercompatfixes.compat.registrate;

import java.util.concurrent.locks.ReentrantLock;

/** Per-owner construction transaction, not a registry-dispatch or user-callback lock. */
public final class RegistrateTransaction {
    private final ReentrantLock lock = new ReentrantLock();
    public int baseline() { return lock.getHoldCount(); }
    public void begin() { lock.lock(); }
    public void end() { lock.unlock(); }
    public void restore(int baseline) {
        while (lock.getHoldCount() > baseline) lock.unlock();
    }
}

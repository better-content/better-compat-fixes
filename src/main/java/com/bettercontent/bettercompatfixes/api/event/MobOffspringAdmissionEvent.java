package com.bettercontent.bettercompatfixes.api.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Causal, fresh insertion only. Completion is delivered once, including vetoes and exceptions. */
@Cancelable
public final class MobOffspringAdmissionEvent extends Event {
    private final Entity parent;
    private final Mob child;
    private final List<Consumer<Boolean>> completions = new ArrayList<>();
    private boolean completed;

    public MobOffspringAdmissionEvent(Entity parent, Mob child) { this.parent = parent; this.child = child; }
    public Entity parent() { return parent; }
    public Mob child() { return child; }

    public void onCompletion(Consumer<Boolean> completion) {
        if (completed) throw new IllegalStateException("Offspring insertion has already completed");
        completions.add(completion);
    }

    public void complete(boolean inserted) {
        if (completed) throw new IllegalStateException("Offspring insertion completed twice");
        completed = true;
        Throwable failure = null;
        for (Consumer<Boolean> completion : List.copyOf(completions)) {
            try { completion.accept(inserted); }
            catch (RuntimeException | Error error) {
                if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
            }
        }
        // One broken subscriber must not prevent another account from committing/rolling back.
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }
}

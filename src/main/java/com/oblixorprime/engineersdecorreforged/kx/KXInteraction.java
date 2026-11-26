package com.oblixorprime.engineersdecorreforged.kx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class KXInteraction {
    private final Map<UUID, Start> pending = new HashMap<>();

    public record Endpoint(KXPlacement.Cell pillar, KXOrientation orientation,
            KXTopology.Slot port, String plane, String dimension) {
        public Endpoint {
            Objects.requireNonNull(pillar);Objects.requireNonNull(orientation);
            Objects.requireNonNull(port);Objects.requireNonNull(plane);Objects.requireNonNull(dimension);
            if(plane.isBlank() || dimension.isBlank())throw new IllegalArgumentException("Missing plane/dimension");
        }
    }
    private record Start(Endpoint endpoint) {}

    public synchronized void begin(UUID user,Endpoint source) {
        Objects.requireNonNull(user);Objects.requireNonNull(source);
        pending.put(user,new Start(source));
    }
    public synchronized boolean hasPending(UUID user) { return pending.containsKey(user); }

    /** A Span carries one slot. Different explicitly selected endpoint slots are rejected, never guessed. */
    public synchronized KXPlacement.Plan preview(UUID user,Endpoint target) {
        Start start=pending.get(Objects.requireNonNull(user));
        if(start==null)throw new IllegalStateException("No selected source pillar");
        Endpoint from=start.endpoint();
        Objects.requireNonNull(target);
        if(!from.dimension().equals(target.dimension()))throw new IllegalArgumentException("Different dimensions");
        if(!from.orientation().equals(target.orientation()))throw new IllegalArgumentException("Different orientation");
        if(!from.plane().equals(target.plane()))throw new IllegalArgumentException("Different bracing plane");
        if(from.pillar().equals(target.pillar()))throw new IllegalArgumentException("A span cannot end at its own pillar");
        if(from.port()!=target.port())
            throw new IllegalArgumentException("Ambiguous source/target port pairing: the current Span schema cannot represent two different slots");
        return KXPlacement.plan(from.orientation(),from.plane(),java.util.List.of(
                new KXPlacement.Span(from.pillar(),target.pillar(),from.port(),from.plane())));
    }

    public synchronized KXRuntimePlacement.Result confirm(UUID user,Endpoint target,
            KXPlacement.World world,KXRuntimePlacement.SyncPort sync) {
        Objects.requireNonNull(world,"world");
        Objects.requireNonNull(sync,"sync");
        final KXPlacement.Plan plan;
        try {
            plan=preview(user,target);
        } catch(RuntimeException ex) {
            var rejected=new KXPlacement.Result(KXPlacement.Status.REJECTED,0,0,
                    Objects.requireNonNullElse(ex.getMessage(),"invalid K/X endpoint"));
            return new KXRuntimePlacement.Result(rejected,KXRuntimePlacement.SyncState.NOT_NEEDED,Set.of());
        }
        var result=KXRuntimePlacement.apply(world,plan,sync);
        if(result.placement().status()==KXPlacement.Status.APPLIED
                || result.placement().status()==KXPlacement.Status.RECOVERY_REQUIRED) {
            pending.remove(user);
        }
        return result;
    }
    public synchronized void cancel(UUID user) { pending.remove(user); }
    public synchronized void clear(UUID user) { pending.remove(user); }
}

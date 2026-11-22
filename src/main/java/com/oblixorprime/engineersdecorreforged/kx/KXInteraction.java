package com.oblixorprime.engineersdecorreforged.kx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Server-owned, two-endpoint interaction state machine. No keybind is assumed:
 * the mod's actual Item.useOn / client ghost UI decides which click starts,
 * previews, confirms or cancels an operation.
 * No persistent player tracking or background work.
 */
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

    /** First click selects an existing pillar socket, without placing or charging an item. */
    public synchronized void begin(UUID user,Endpoint source) {
        Objects.requireNonNull(user);Objects.requireNonNull(source);
        pending.put(user,new Start(source));
    }
    public synchronized boolean hasPending(UUID user) { return pending.containsKey(user); }
    /** Client preview may call this; it does not mutate the world. */
    public synchronized KXPlacement.Plan preview(UUID user,Endpoint target) {
        Start start=pending.get(Objects.requireNonNull(user));
        if(start==null)throw new IllegalStateException("No selected source pillar");
        Endpoint from=start.endpoint();
        Objects.requireNonNull(target);
        if(!from.dimension().equals(target.dimension()))throw new IllegalArgumentException("Different dimensions");
        if(!from.orientation().equals(target.orientation()))throw new IllegalArgumentException("Different orientation");
        if(!from.plane().equals(target.plane()))throw new IllegalArgumentException("Different bracing plane");
        if(from.pillar().equals(target.pillar()))throw new IllegalArgumentException("A span cannot end at its own pillar");
        return KXPlacement.plan(from.orientation(),from.plane(),java.util.List.of(
                new KXPlacement.Span(from.pillar(),target.pillar(),from.port(),from.plane())));
    }
    /**
     * Server confirmation. Placement and its topology/client invalidation are one
     * integration path; callers must provide the real game adapter's sync port.
     */
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
        // A committed placement is final even when sync is pending. An incomplete
        // rollback is also non-retryable because the world may require recovery.
        if(result.placement().status()==KXPlacement.Status.APPLIED
                || result.placement().status()==KXPlacement.Status.RECOVERY_REQUIRED) {
            pending.remove(user);
        }
        return result;
    }
    public synchronized void cancel(UUID user) { pending.remove(user); }
    /** Call on logout/world transfer; never save a half-completed client transaction. */
    public synchronized void clear(UUID user) { pending.remove(user); }
}

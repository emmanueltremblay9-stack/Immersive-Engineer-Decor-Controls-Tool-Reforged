package com.oblixorprime.engineersdecorreforged.stool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Invisible, non-persistent mount. The stool block remains the only selectable/collidable object. */
public final class StoolSeatEntity extends Entity {
   public static final double SEAT_HEIGHT = 10.0 / 16.0;
   private boolean loadedFromDisk;

   public StoolSeatEntity(EntityType<? extends StoolSeatEntity> type, Level level) {
      super(type, level);
      noPhysics = true;
      setNoGravity(true);
      setInvulnerable(true);
      setInvisible(true);
   }

   public boolean isValidSeatFor(Player player) {
      return !isRemoved() && !loadedFromDisk && player.isAlive() && !player.isRemoved() && !player.isSpectator()
         && player.level() == level() && getPassengers().size() == 1 && hasPassenger(player)
         && level().getBlockState(blockPosition()).getBlock() instanceof TreatedWoodStoolBlock;
   }

   public boolean hasRoomFor(Player player) {
      Vec3 feet = getPassengerRidingPosition(player).subtract(player.getVehicleAttachmentPoint(this));
      AABB body = player.getDimensions(Pose.STANDING).makeBoundingBox(feet);
      // The bent legs overlap the stool; only its own seated occupant ignores that collision.
      AABB headroom = new AABB(body.minX, Math.max(body.minY, getY() + 0.001), body.minZ,
         body.maxX, body.maxY, body.maxZ);
      return level().noCollision(player, headroom);
   }

   @Override
   protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
      return Vec3.ZERO;
   }

   @Override
   protected boolean canAddPassenger(Entity passenger) {
      return passenger instanceof Player player && player.isAlive() && !player.isSpectator()
         && player.getPose() == Pose.STANDING && getPassengers().isEmpty() && !isRemoved()
         && level().getBlockState(blockPosition()).getBlock() instanceof TreatedWoodStoolBlock && hasRoomFor(player);
   }

   @Override
   public void tick() {
      if (!level().isClientSide && (loadedFromDisk || !(getFirstPassenger() instanceof Player player)
         || !isValidSeatFor(player))) {
         discard();
      }
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!level().isClientSide) {
         // Native dismount skips the vehicle's safe-location helper once it is marked removed.
         // Eject first, while it is alive; ordinary empty seats are cleaned on the next tick.
         ejectPassengers();
      }
      super.remove(reason);
   }

   @Override
   public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
      BlockPos origin = blockPosition();
      for (Direction direction : Direction.Plane.HORIZONTAL) {
         Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), origin.relative(direction), true);
         if (safe != null) {
            return safe;
         }
      }
      Vec3 above = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), origin.above(), true);
      return above != null ? above : super.getDismountLocationForPassenger(passenger);
   }

   @Override
   public boolean isPickable() {
      return false;
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public boolean canBeCollidedWith() {
      return false;
   }

   @Override
   public void setDeltaMovement(Vec3 movement) {
      // Explosions, currents and entity pushes must never move a stationary seat.
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      // EntityType.noSave() prevents this temporary vehicle being saved with chunks or players.
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      // Defensive cleanup if old/externally-created NBT attempts to restore a seat.
      loadedFromDisk = true;
   }
}

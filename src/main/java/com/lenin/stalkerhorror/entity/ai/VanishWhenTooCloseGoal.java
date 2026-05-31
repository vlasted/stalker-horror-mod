package com.lenin.stalkerhorror.entity.ai;

import com.lenin.stalkerhorror.entity.StalkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class VanishWhenTooCloseGoal extends Goal {
    private final StalkerEntity stalker;
    private final double vanishDistance;
    private final double reappearDistance;
    private Player targetPlayer;

    public VanishWhenTooCloseGoal(StalkerEntity stalker, double vanishDistance, double reappearDistance) {
        this.stalker = stalker;
        this.vanishDistance = vanishDistance;
        this.reappearDistance = reappearDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.stalker.canVanish()) {
            return false;
        }

        this.targetPlayer = this.stalker.level().getNearestPlayer(
                TargetingConditions.forNonCombat()
                        .range(this.vanishDistance)
                        .selector(entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()),
                this.stalker
        );

        return this.targetPlayer != null && this.stalker.hasLineOfSight(this.targetPlayer);
    }

    @Override
    public void start() {
        this.stalker.getNavigation().stop();

        if (this.tryTeleportNearPlayer()) {
            this.stalker.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.55F);
            this.stalker.resetVanishCooldown();
        }

        this.targetPlayer = null;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    private boolean tryTeleportNearPlayer() {
        if (!(this.stalker.level() instanceof ServerLevel serverLevel) || this.targetPlayer == null) {
            return false;
        }

        Vec3 look = this.targetPlayer.getViewVector(1.0F).normalize();
        Vec3 behind = this.targetPlayer.position().subtract(look.scale(this.reappearDistance));
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();

        for (int attempt = 0; attempt < 16; attempt++) {
            double sideOffset = (this.stalker.getRandom().nextDouble() - 0.5D) * 8.0D;
            double distanceOffset = this.stalker.getRandom().nextDouble() * 4.0D;

            double x = behind.x + side.x * sideOffset - look.x * distanceOffset;
            double z = behind.z + side.z * sideOffset - look.z * distanceOffset;
            double y = this.targetPlayer.getY() + this.stalker.getRandom().nextInt(5) - 2;

            BlockPos safePosition = this.findSafePosition(serverLevel, x, y, z);

            if (safePosition != null) {
                this.stalker.teleportTo(
                        safePosition.getX() + 0.5D,
                        safePosition.getY(),
                        safePosition.getZ() + 0.5D
                );
                this.stalker.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);
                return true;
            }
        }

        return false;
    }

    private BlockPos findSafePosition(ServerLevel serverLevel, double x, double y, double z) {
        BlockPos basePosition = BlockPos.containing(x, y, z);

        for (int verticalOffset = 3; verticalOffset >= -4; verticalOffset--) {
            BlockPos feetPosition = basePosition.offset(0, verticalOffset, 0);
            BlockPos headPosition = feetPosition.above();
            BlockPos floorPosition = feetPosition.below();

            boolean hasFloor = serverLevel.getBlockState(floorPosition)
                    .isFaceSturdy(serverLevel, floorPosition, Direction.UP);

            boolean hasSpace = serverLevel.getBlockState(feetPosition).isAir()
                    && serverLevel.getBlockState(headPosition).isAir();

            if (hasFloor && hasSpace) {
                return feetPosition;
            }
        }

        return null;
    }
}
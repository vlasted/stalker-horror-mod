package com.lenin.stalkerhorror.entity.ai;

import com.lenin.stalkerhorror.entity.StalkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

public class DisturbDoorGoal extends Goal {
    private final StalkerEntity stalker;
    private final double playerDetectionDistance;
    private Player targetPlayer;

    public DisturbDoorGoal(StalkerEntity stalker, double playerDetectionDistance) {
        this.stalker = stalker;
        this.playerDetectionDistance = playerDetectionDistance;
    }

    @Override
    public boolean canUse() {
        if (!this.stalker.canDisturbDoor()) {
            return false;
        }

        this.targetPlayer = this.stalker.level().getNearestPlayer(
                TargetingConditions.forNonCombat()
                        .range(this.playerDetectionDistance)
                        .selector(entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()),
                this.stalker
        );

        return this.targetPlayer != null;
    }

    @Override
    public void start() {
        if (!(this.stalker.level() instanceof ServerLevel serverLevel) || this.targetPlayer == null) {
            return;
        }

        BlockPos disturbedPos = this.stalker.tryDisturbDoorNearPlayer(serverLevel, this.targetPlayer);

        if (disturbedPos != null) {
            serverLevel.playSound(
                    null,
                    disturbedPos,
                    SoundEvents.WOODEN_DOOR_OPEN,
                    this.stalker.getSoundSource(),
                    0.75F,
                    0.55F
            );

            this.stalker.resetDoorDisturbanceCooldown();
        } else {
            this.stalker.setShortDoorDisturbanceCooldown();
        }

        this.targetPlayer = null;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
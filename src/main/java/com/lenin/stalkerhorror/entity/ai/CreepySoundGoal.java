package com.lenin.stalkerhorror.entity.ai;

import com.lenin.stalkerhorror.entity.StalkerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

public class CreepySoundGoal extends Goal {
    private final StalkerEntity stalker;
    private final double playerDetectionDistance;
    private Player targetPlayer;

    public CreepySoundGoal(StalkerEntity stalker, double playerDetectionDistance) {
        this.stalker = stalker;
        this.playerDetectionDistance = playerDetectionDistance;
    }

    @Override
    public boolean canUse() {
        if (!this.stalker.canPlayCreepySound()) {
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

        this.stalker.playCreepySoundNearPlayer(serverLevel, this.targetPlayer);
        this.stalker.resetCreepySoundCooldown();

        this.targetPlayer = null;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
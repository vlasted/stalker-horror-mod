package com.lenin.stalkerhorror.entity.ai;

import com.lenin.stalkerhorror.entity.StalkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

public class DarkenAreaGoal extends Goal {
    private final StalkerEntity stalker;
    private final double playerDetectionDistance;
    private Player targetPlayer;

    public DarkenAreaGoal(StalkerEntity stalker, double playerDetectionDistance) {
        this.stalker = stalker;
        this.playerDetectionDistance = playerDetectionDistance;
    }

    @Override
    public boolean canUse() {
        if (!this.stalker.canDarkenArea()) {
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

        BlockPos darkenedPos = this.stalker.tryDarkenLightNearPlayer(serverLevel, this.targetPlayer);

        if (darkenedPos != null) {
            serverLevel.playSound(
                    null,
                    darkenedPos,
                    SoundEvents.FIRE_EXTINGUISH,
                    this.stalker.getSoundSource(),
                    0.6F,
                    0.55F
            );

            this.stalker.resetDarkenCooldown();
        }

        this.targetPlayer = null;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
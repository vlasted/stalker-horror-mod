package com.lenin.stalkerhorror.entity.ai;

import com.lenin.stalkerhorror.entity.StalkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

public class ManipulateBlockGoal extends Goal {
    private final StalkerEntity stalker;
    private final double playerDetectionDistance;
    private Player targetPlayer;

    public ManipulateBlockGoal(StalkerEntity stalker, double playerDetectionDistance) {
        this.stalker = stalker;
        this.playerDetectionDistance = playerDetectionDistance;
    }

    @Override
    public boolean canUse() {
        if (!this.stalker.canManipulateBlocks()) {
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

        BlockPos stolenPos = this.stalker.tryStealBlockNearPlayer(serverLevel, this.targetPlayer);

        if (stolenPos != null) {
            serverLevel.playSound(
                    null,
                    stolenPos,
                    SoundEvents.WOOD_BREAK,
                    this.stalker.getSoundSource(),
                    0.45F,
                    0.55F
            );

            this.stalker.resetBlockManipulationCooldown();
        }

        this.targetPlayer = null;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
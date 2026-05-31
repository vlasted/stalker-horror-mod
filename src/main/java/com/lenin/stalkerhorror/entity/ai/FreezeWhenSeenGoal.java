package com.lenin.stalkerhorror.entity.ai;

import com.lenin.stalkerhorror.util.PlayerLookUtils;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FreezeWhenSeenGoal extends Goal {
    private final Monster stalker;
    private final double freezeDistance;
    private final double lookThreshold;
    private Player targetPlayer;

    public FreezeWhenSeenGoal(Monster stalker, double freezeDistance, double lookThreshold) {
        this.stalker = stalker;
        this.freezeDistance = freezeDistance;
        this.lookThreshold = lookThreshold;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.targetPlayer = this.stalker.level().getNearestPlayer(
                TargetingConditions.forNonCombat()
                        .range(this.freezeDistance)
                        .selector(entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()),
                this.stalker
        );

        return this.targetPlayer != null
                && PlayerLookUtils.isPlayerLookingAtEntity(this.targetPlayer, this.stalker, this.freezeDistance, this.lookThreshold);
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPlayer != null
                && this.targetPlayer.isAlive()
                && PlayerLookUtils.isPlayerLookingAtEntity(this.targetPlayer, this.stalker, this.freezeDistance, this.lookThreshold);
    }

    @Override
    public void start() {
        this.stalker.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.stalker.getNavigation().stop();
        this.stalker.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
    }
}
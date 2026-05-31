package com.lenin.stalkerhorror.entity.ai;

import com.lenin.stalkerhorror.util.PlayerLookUtils;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class StalkPlayerGoal extends Goal {
    private final Monster stalker;
    private final double speedModifier;
    private final double minDistance;
    private final double maxDistance;
    private Player targetPlayer;

    public StalkPlayerGoal(Monster stalker, double speedModifier, double minDistance, double maxDistance) {
        this.stalker = stalker;
        this.speedModifier = speedModifier;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.targetPlayer = this.stalker.level().getNearestPlayer(
                TargetingConditions.forNonCombat()
                        .range(this.maxDistance)
                        .selector(entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()),
                this.stalker
        );

        if (this.targetPlayer == null) {
            return false;
        }

        double distance = this.stalker.distanceTo(this.targetPlayer);

        return distance > this.minDistance
                && distance <= this.maxDistance
                && this.stalker.hasLineOfSight(this.targetPlayer)
                && !PlayerLookUtils.isPlayerLookingAtEntity(this.targetPlayer, this.stalker, this.maxDistance, 0.93D);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
            return false;
        }

        double distance = this.stalker.distanceTo(this.targetPlayer);

        return distance > this.minDistance
                && distance <= this.maxDistance
                && !PlayerLookUtils.isPlayerLookingAtEntity(this.targetPlayer, this.stalker, this.maxDistance, 0.93D);
    }

    @Override
    public void tick() {
        this.stalker.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);
        this.stalker.getNavigation().moveTo(this.targetPlayer, this.speedModifier);
    }

    @Override
    public void stop() {
        this.stalker.getNavigation().stop();
        this.targetPlayer = null;
    }
}
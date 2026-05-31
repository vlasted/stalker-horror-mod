package com.lenin.stalkerhorror.entity.ai;

import com.lenin.stalkerhorror.entity.StalkerEntity;
import com.lenin.stalkerhorror.util.PlayerLookUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class VanishWhenWatchedGoal extends Goal {
    private final StalkerEntity stalker;
    private final double watchDistance;
    private final double lookThreshold;
    private final int requiredWatchTicks;
    private Player targetPlayer;
    private int watchedTicks;

    public VanishWhenWatchedGoal(StalkerEntity stalker, double watchDistance, double lookThreshold, int requiredWatchTicks) {
        this.stalker = stalker;
        this.watchDistance = watchDistance;
        this.lookThreshold = lookThreshold;
        this.requiredWatchTicks = requiredWatchTicks;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.stalker.canVanish()) {
            return false;
        }

        this.targetPlayer = this.stalker.level().getNearestPlayer(
                TargetingConditions.forNonCombat()
                        .range(this.watchDistance)
                        .selector(entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()),
                this.stalker
        );

        return this.targetPlayer != null
                && PlayerLookUtils.isPlayerLookingAtEntity(this.targetPlayer, this.stalker, this.watchDistance, this.lookThreshold);
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPlayer != null
                && this.targetPlayer.isAlive()
                && this.stalker.canVanish()
                && PlayerLookUtils.isPlayerLookingAtEntity(this.targetPlayer, this.stalker, this.watchDistance, this.lookThreshold);
    }

    @Override
    public void start() {
        this.watchedTicks = 0;
        this.stalker.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.stalker.getNavigation().stop();
        this.stalker.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);

        this.watchedTicks++;

        if (this.watchedTicks >= this.requiredWatchTicks) {
            if (this.stalker.tryRepositionNearPlayer(this.targetPlayer, 10.0D, 18.0D)) {
                this.stalker.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.75F, 0.45F);
                this.stalker.resetVanishCooldown();
            }

            this.targetPlayer = null;
            this.watchedTicks = 0;
        }
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        this.watchedTicks = 0;
    }
}
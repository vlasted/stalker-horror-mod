package com.lenin.stalkerhorror.entity;

import com.lenin.stalkerhorror.entity.ai.FreezeWhenSeenGoal;
import com.lenin.stalkerhorror.entity.ai.ObservePlayerGoal;
import com.lenin.stalkerhorror.entity.ai.StalkPlayerGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class StalkerEntity extends Monster {
    public StalkerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(1, new FreezeWhenSeenGoal(this, 24.0D, 0.93D));
        this.goalSelector.addGoal(2, new StalkPlayerGoal(this, 0.85D, 5.0D, 28.0D));
        this.goalSelector.addGoal(3, new ObservePlayerGoal(this, 32.0D));

        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.45D));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }
}
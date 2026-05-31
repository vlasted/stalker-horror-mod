package com.lenin.stalkerhorror.entity;

import com.lenin.stalkerhorror.entity.ai.ManipulateBlockGoal;
import com.lenin.stalkerhorror.entity.ai.DarkenAreaGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.lenin.stalkerhorror.entity.ai.FreezeWhenSeenGoal;
import com.lenin.stalkerhorror.entity.ai.ObservePlayerGoal;
import com.lenin.stalkerhorror.entity.ai.StalkPlayerGoal;
import com.lenin.stalkerhorror.entity.ai.VanishWhenTooCloseGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class StalkerEntity extends Monster {
    private int vanishCooldown;
    private ItemStack stolenBlockItem = ItemStack.EMPTY;
    private int blockManipulationCooldown;
    private int darkenCooldown;

    public StalkerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.vanishCooldown = 160;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(1, new VanishWhenTooCloseGoal(this, 3.0D, 12.0D));
        this.goalSelector.addGoal(2, new FreezeWhenSeenGoal(this, 24.0D, 0.93D));
        this.goalSelector.addGoal(3, new DarkenAreaGoal(this, 18.0D));
        this.goalSelector.addGoal(3, new ManipulateBlockGoal(this, 18.0D));
        this.goalSelector.addGoal(3, new StalkPlayerGoal(this, 0.85D, 5.0D, 28.0D));
        this.goalSelector.addGoal(4, new ObservePlayerGoal(this, 32.0D));

        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.45D));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.vanishCooldown > 0) {
            this.vanishCooldown--;
        }

        if (this.blockManipulationCooldown > 0) {
            this.blockManipulationCooldown--;
        }

        if (this.darkenCooldown > 0) {
            this.darkenCooldown--;
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, lootingLevel, recentlyHit);

        if (!this.stolenBlockItem.isEmpty()) {
            Containers.dropItemStack(this.level(), this.getX(), this.getY(), this.getZ(), this.stolenBlockItem.copy());
            this.stolenBlockItem = ItemStack.EMPTY;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        compound.putInt("VanishCooldown", this.vanishCooldown);
        compound.putInt("BlockManipulationCooldown", this.blockManipulationCooldown);
        compound.putInt("DarkenCooldown", this.darkenCooldown);

        if (!this.stolenBlockItem.isEmpty()) {
            CompoundTag stolenBlockTag = new CompoundTag();
            this.stolenBlockItem.save(stolenBlockTag);
            compound.put("StolenBlockItem", stolenBlockTag);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        this.vanishCooldown = compound.getInt("VanishCooldown");
        this.blockManipulationCooldown = compound.getInt("BlockManipulationCooldown");
        this.darkenCooldown = compound.getInt("DarkenCooldown");

        if (compound.contains("StolenBlockItem")) {
            this.stolenBlockItem = ItemStack.of(compound.getCompound("StolenBlockItem"));
        } else {
            this.stolenBlockItem = ItemStack.EMPTY;
        }
    }

    public boolean canVanish() {
        return this.vanishCooldown <= 0;
    }

    public void resetVanishCooldown() {
        this.vanishCooldown = 160;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public boolean canManipulateBlocks() {
        return this.blockManipulationCooldown <= 0 && this.stolenBlockItem.isEmpty();
    }

    public void resetBlockManipulationCooldown() {
        this.blockManipulationCooldown = 900;
    }

    public BlockPos tryStealBlockNearPlayer(ServerLevel serverLevel, Player player) {
        BlockPos playerPos = player.blockPosition();

        for (int attempt = 0; attempt < 32; attempt++) {
            int xOffset = this.getRandom().nextInt(11) - 5;
            int yOffset = this.getRandom().nextInt(5) - 2;
            int zOffset = this.getRandom().nextInt(11) - 5;

            BlockPos targetPos = playerPos.offset(xOffset, yOffset, zOffset);
            BlockState state = serverLevel.getBlockState(targetPos);

            if (!this.canStealBlock(serverLevel, targetPos, state)) {
                continue;
            }

            ItemStack stolenStack = new ItemStack(state.getBlock().asItem());

            if (stolenStack.isEmpty() || stolenStack.is(Items.AIR)) {
                continue;
            }

            serverLevel.levelEvent(2001, targetPos, Block.getId(state));
            serverLevel.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);

            this.stolenBlockItem = stolenStack;
            return targetPos;
        }

        this.blockManipulationCooldown = 200;
        return null;
    }

    private boolean canStealBlock(ServerLevel serverLevel, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        if (state.hasBlockEntity()) {
            return false;
        }

        if (state.getDestroySpeed(serverLevel, pos) < 0.0F) {
            return false;
        }

        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER)
                || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.ENCHANTING_TABLE)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL)
                || state.is(Blocks.DAMAGED_ANVIL)
                || state.is(Blocks.BEACON)
                || state.is(Blocks.SPAWNER)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.NETHER_PORTAL)) {
            return false;
        }

        return true;
    }

    public boolean canDarkenArea() {
        return this.darkenCooldown <= 0 && this.stolenBlockItem.isEmpty();
    }

    public void resetDarkenCooldown() {
        this.darkenCooldown = 1200;
    }

    public BlockPos tryDarkenLightNearPlayer(ServerLevel serverLevel, Player player) {
        BlockPos playerPos = player.blockPosition();

        for (int attempt = 0; attempt < 32; attempt++) {
            int xOffset = this.getRandom().nextInt(13) - 6;
            int yOffset = this.getRandom().nextInt(7) - 3;
            int zOffset = this.getRandom().nextInt(13) - 6;

            BlockPos targetPos = playerPos.offset(xOffset, yOffset, zOffset);
            BlockState state = serverLevel.getBlockState(targetPos);

            if (!this.canRemoveLightBlock(state)) {
                continue;
            }

            ItemStack lightItem = new ItemStack(state.getBlock().asItem());

            if (!lightItem.isEmpty()) {
                this.stolenBlockItem = lightItem;
            }

            serverLevel.levelEvent(2001, targetPos, net.minecraft.world.level.block.Block.getId(state));
            serverLevel.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);

            return targetPos;
        }

        this.darkenCooldown = 300;
        return null;
    }

    private boolean canRemoveLightBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.hasBlockEntity()) {
            return false;
        }

        return state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.REDSTONE_TORCH)
                || state.is(Blocks.REDSTONE_WALL_TORCH)
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.SOUL_LANTERN);
    }

}
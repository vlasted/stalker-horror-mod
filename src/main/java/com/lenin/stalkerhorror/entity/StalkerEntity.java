package com.lenin.stalkerhorror.entity;

import com.lenin.stalkerhorror.entity.ai.DisturbDoorGoal;
import com.lenin.stalkerhorror.entity.ai.ManipulateBlockGoal;
import com.lenin.stalkerhorror.entity.ai.DarkenAreaGoal;
import net.minecraft.core.BlockPos;
import com.lenin.stalkerhorror.entity.ai.CreepySoundGoal;
import com.lenin.stalkerhorror.entity.ai.VanishWhenWatchedGoal;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
    private int creepySoundCooldown;
    private int doorDisturbanceCooldown;

    public StalkerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.vanishCooldown = 160;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        this.goalSelector.addGoal(1, new VanishWhenTooCloseGoal(this, 3.0D, 12.0D));
        this.goalSelector.addGoal(2, new VanishWhenWatchedGoal(this, 26.0D, 0.96D, 60));
        this.goalSelector.addGoal(2, new FreezeWhenSeenGoal(this, 24.0D, 0.93D));
        this.goalSelector.addGoal(3, new CreepySoundGoal(this, 28.0D));
        this.goalSelector.addGoal(3, new DarkenAreaGoal(this, 18.0D));
        this.goalSelector.addGoal(5, new DisturbDoorGoal(this, 18.0D));
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

        if (this.creepySoundCooldown > 0) {
            this.creepySoundCooldown--;
        }

        if (this.doorDisturbanceCooldown > 0) {
            this.doorDisturbanceCooldown--;
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
        compound.putInt("CreepySoundCooldown", this.creepySoundCooldown);
        compound.putInt("DoorDisturbanceCooldown", this.doorDisturbanceCooldown);

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
        this.creepySoundCooldown = compound.getInt("CreepySoundCooldown");
        this.doorDisturbanceCooldown = compound.getInt("DoorDisturbanceCooldown");

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

    public boolean tryRepositionNearPlayer(Player player, double minDistance, double maxDistance) {
        if (!(this.level() instanceof ServerLevel serverLevel) || player == null) {
            return false;
        }

        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();

        for (int attempt = 0; attempt < 24; attempt++) {
            double distance = minDistance + this.getRandom().nextDouble() * (maxDistance - minDistance);
            double sideOffset = (this.getRandom().nextDouble() - 0.5D) * 8.0D;

            Vec3 basePosition = player.position()
                    .subtract(look.scale(distance))
                    .add(side.scale(sideOffset));

            double y = player.getY() + this.getRandom().nextInt(5) - 2;

            BlockPos safePosition = this.findSafeRepositionPosition(
                    serverLevel,
                    basePosition.x,
                    y,
                    basePosition.z
            );

            if (safePosition == null) {
                continue;
            }

            this.teleportTo(
                    safePosition.getX() + 0.5D,
                    safePosition.getY(),
                    safePosition.getZ() + 0.5D
            );

            this.getNavigation().stop();
            this.getLookControl().setLookAt(player, 30.0F, 30.0F);

            return true;
        }

        return false;
    }

    private BlockPos findSafeRepositionPosition(ServerLevel serverLevel, double x, double y, double z) {
        BlockPos basePosition = BlockPos.containing(x, y, z);

        for (int verticalOffset = 4; verticalOffset >= -5; verticalOffset--) {
            BlockPos feetPosition = basePosition.offset(0, verticalOffset, 0);
            BlockPos headPosition = feetPosition.above();
            BlockPos floorPosition = feetPosition.below();

            boolean hasFloor = serverLevel.getBlockState(floorPosition)
                    .isFaceSturdy(serverLevel, floorPosition, Direction.UP);

            boolean hasSpace = serverLevel.getBlockState(feetPosition).isAir()
                    && serverLevel.getBlockState(headPosition).isAir();

            boolean hasNoFluid = serverLevel.getBlockState(feetPosition).getFluidState().isEmpty()
                    && serverLevel.getBlockState(headPosition).getFluidState().isEmpty();

            if (hasFloor && hasSpace && hasNoFluid) {
                return feetPosition;
            }
        }

        return null;
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

    public boolean canPlayCreepySound() {
        return this.creepySoundCooldown <= 0;
    }

    public void resetCreepySoundCooldown() {
        this.creepySoundCooldown = 240 + this.getRandom().nextInt(361);
    }

    public void playCreepySoundNearPlayer(ServerLevel serverLevel, Player player) {
        BlockPos playerPos = player.blockPosition();

        int xOffset = this.getRandom().nextInt(17) - 8;
        int yOffset = this.getRandom().nextInt(5) - 2;
        int zOffset = this.getRandom().nextInt(17) - 8;

        BlockPos soundPos = playerPos.offset(xOffset, yOffset, zOffset);

        SoundEvent sound = this.getRandomCreepySound();

        float volume = 0.35F + this.getRandom().nextFloat() * 0.35F;
        float pitch = 0.45F + this.getRandom().nextFloat() * 0.35F;

        serverLevel.playSound(
                null,
                soundPos,
                sound,
                SoundSource.HOSTILE,
                volume,
                pitch
        );
    }

    private SoundEvent getRandomCreepySound() {
        int soundIndex = this.getRandom().nextInt(5);

        return switch (soundIndex) {
            case 0 -> SoundEvents.AMBIENT_CAVE.value();
            case 1 -> SoundEvents.ENDERMAN_STARE;
            case 2 -> SoundEvents.ENDERMAN_AMBIENT;
            case 3 -> SoundEvents.ZOMBIE_AMBIENT;
            default -> SoundEvents.SKELETON_AMBIENT;
        };
    }

    public boolean canDisturbDoor() {
        return this.doorDisturbanceCooldown <= 0;
    }

    public void resetDoorDisturbanceCooldown() {
        this.doorDisturbanceCooldown = 500 + this.getRandom().nextInt(501);
    }

    public void setShortDoorDisturbanceCooldown() {
        this.doorDisturbanceCooldown = 160;
    }

    public BlockPos tryDisturbDoorNearPlayer(ServerLevel serverLevel, Player player) {
        BlockPos playerPos = player.blockPosition();

        for (int attempt = 0; attempt < 48; attempt++) {
            int xOffset = this.getRandom().nextInt(15) - 7;
            int yOffset = this.getRandom().nextInt(7) - 3;
            int zOffset = this.getRandom().nextInt(15) - 7;

            BlockPos targetPos = playerPos.offset(xOffset, yOffset, zOffset);
            BlockState state = serverLevel.getBlockState(targetPos);

            if (!this.canDisturbOpenableBlock(state)) {
                continue;
            }

            boolean currentOpen = state.getValue(BlockStateProperties.OPEN);
            BlockState newState = state.setValue(BlockStateProperties.OPEN, !currentOpen);

            serverLevel.setBlock(targetPos, newState, 3);
            this.updateOtherDoorHalfIfNeeded(serverLevel, targetPos, state, !currentOpen);

            return targetPos;
        }

        return null;
    }

    private boolean canDisturbOpenableBlock(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.hasBlockEntity()) {
            return false;
        }

        if (!state.hasProperty(BlockStateProperties.OPEN)) {
            return false;
        }

        Block block = state.getBlock();

        return block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock;
    }

    private void updateOtherDoorHalfIfNeeded(ServerLevel serverLevel, BlockPos pos, BlockState state, boolean open) {
        if (!(state.getBlock() instanceof DoorBlock)) {
            return;
        }

        if (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return;
        }

        DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
        BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState otherState = serverLevel.getBlockState(otherPos);

        if (otherState.getBlock() == state.getBlock() && otherState.hasProperty(BlockStateProperties.OPEN)) {
            serverLevel.setBlock(otherPos, otherState.setValue(BlockStateProperties.OPEN, open), 3);
        }
    }

}
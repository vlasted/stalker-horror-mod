package com.lenin.stalkerhorror.event;

import java.util.Comparator;
import java.util.List;
import com.lenin.stalkerhorror.StalkerHorrorMod;
import com.lenin.stalkerhorror.entity.StalkerEntity;
import com.lenin.stalkerhorror.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = StalkerHorrorMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class StalkerSpawnHandler {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int SPAWN_COOLDOWN_TICKS = 400;
    private static final int FAILED_ATTEMPT_COOLDOWN_TICKS = 100;

    private static final double MIN_SPAWN_DISTANCE = 18.0D;
    private static final double MAX_SPAWN_DISTANCE = 38.0D;
    private static final double NEARBY_STALKER_RADIUS = 56.0D;

    private static final int MAX_SPAWN_ATTEMPTS = 48;

    private static int tickCounter = 0;
    private static final Map<UUID, Integer> playerCooldowns = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;

        if (tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }

        tickCounter = 0;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return;
        }

        cleanupExtraStalkersAroundPlayers(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            handlePlayerSpawnCheck(player);
        }
    }

    private static void handlePlayerSpawnCheck(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
            return;
        }

        ServerLevel serverLevel = player.serverLevel();

        if (serverLevel.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        UUID playerId = player.getUUID();
        int cooldown = playerCooldowns.getOrDefault(playerId, 0);

        if (cooldown > 0) {
            playerCooldowns.put(playerId, Math.max(0, cooldown - CHECK_INTERVAL_TICKS));
            return;
        }

        if (hasNearbyStalker(serverLevel, player)) {
            playerCooldowns.put(playerId, FAILED_ATTEMPT_COOLDOWN_TICKS);
            return;
        }

        BlockPos spawnPos = findHiddenDarkSpawnPosition(serverLevel, player);

        if (spawnPos == null) {
            playerCooldowns.put(playerId, FAILED_ATTEMPT_COOLDOWN_TICKS);
            return;
        }

        if (spawnStalker(serverLevel, spawnPos)) {
            playerCooldowns.put(playerId, SPAWN_COOLDOWN_TICKS);
        } else {
            playerCooldowns.put(playerId, FAILED_ATTEMPT_COOLDOWN_TICKS);
        }
    }

    private static boolean hasNearbyStalker(ServerLevel serverLevel, ServerPlayer player) {
        AABB searchArea = new AABB(player.blockPosition()).inflate(NEARBY_STALKER_RADIUS);

        return !serverLevel.getEntitiesOfClass(
                StalkerEntity.class,
                searchArea,
                stalker -> stalker.isAlive()
        ).isEmpty();
    }

    private static BlockPos findHiddenDarkSpawnPosition(ServerLevel serverLevel, ServerPlayer player) {
        RandomSource random = player.getRandom();
        BlockPos playerPos = player.blockPosition();

        for (int attempt = 0; attempt < MAX_SPAWN_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);

            double x = playerPos.getX() + Math.cos(angle) * distance;
            double z = playerPos.getZ() + Math.sin(angle) * distance;
            int y = playerPos.getY() + random.nextInt(9) - 4;

            BlockPos basePos = BlockPos.containing(x, y, z);
            BlockPos safePos = findSafeGroundPosition(serverLevel, basePos);

            if (safePos == null) {
                continue;
            }

            if (!isDarkEnough(serverLevel, safePos)) {
                continue;
            }

            if (!isHiddenEnough(serverLevel, player, safePos)) {
                continue;
            }

            return safePos;
        }

        return null;
    }

    private static void cleanupExtraStalkersAroundPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }

            ServerLevel serverLevel = player.serverLevel();
            AABB searchArea = new AABB(player.blockPosition()).inflate(NEARBY_STALKER_RADIUS);

            List<StalkerEntity> nearbyStalkers = serverLevel.getEntitiesOfClass(
                    StalkerEntity.class,
                    searchArea,
                    StalkerEntity::isAlive
            );

            if (nearbyStalkers.size() <= 1) {
                continue;
            }

            nearbyStalkers.sort(Comparator.comparingDouble(stalker -> stalker.distanceToSqr(player)));

            for (int index = 1; index < nearbyStalkers.size(); index++) {
                nearbyStalkers.get(index).discard();
            }
        }
    }

    private static BlockPos findSafeGroundPosition(ServerLevel serverLevel, BlockPos basePos) {
        for (int verticalOffset = 4; verticalOffset >= -5; verticalOffset--) {
            BlockPos feetPos = basePos.offset(0, verticalOffset, 0);
            BlockPos headPos = feetPos.above();
            BlockPos floorPos = feetPos.below();

            boolean hasFloor = serverLevel.getBlockState(floorPos)
                    .isFaceSturdy(serverLevel, floorPos, Direction.UP);

            boolean hasSpace = serverLevel.getBlockState(feetPos).isAir()
                    && serverLevel.getBlockState(headPos).isAir();

            boolean hasNoFluid = serverLevel.getBlockState(feetPos).getFluidState().isEmpty()
                    && serverLevel.getBlockState(headPos).getFluidState().isEmpty();

            if (hasFloor && hasSpace && hasNoFluid) {
                return feetPos;
            }
        }

        return null;
    }

    private static boolean isDarkEnough(ServerLevel serverLevel, BlockPos pos) {
        int blockLight = serverLevel.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = serverLevel.getBrightness(LightLayer.SKY, pos);
        boolean covered = !serverLevel.canSeeSky(pos.above());

        return blockLight <= 7 && (covered || skyLight <= 7 || !serverLevel.isDay());
    }

    private static boolean isHiddenEnough(ServerLevel serverLevel, ServerPlayer player, BlockPos spawnPos) {
        if (!isInFrontOfPlayer(player, spawnPos)) {
            return true;
        }

        return hasBlockBetweenPlayerAndSpawn(serverLevel, player, spawnPos);
    }

    private static boolean isInFrontOfPlayer(ServerPlayer player, BlockPos spawnPos) {
        Vec3 playerView = player.getViewVector(1.0F).normalize();
        Vec3 directionToSpawn = Vec3.atCenterOf(spawnPos)
                .subtract(player.getEyePosition())
                .normalize();

        double dot = playerView.dot(directionToSpawn);

        return dot > 0.35D;
    }

    private static boolean hasBlockBetweenPlayerAndSpawn(ServerLevel serverLevel, ServerPlayer player, BlockPos spawnPos) {
        Vec3 start = player.getEyePosition();
        Vec3 end = Vec3.atCenterOf(spawnPos).add(0.0D, 1.0D, 0.0D);

        ClipContext context = new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );

        return serverLevel.clip(context).getType() != HitResult.Type.MISS;
    }

    private static boolean spawnStalker(ServerLevel serverLevel, BlockPos spawnPos) {
        StalkerEntity stalker = ModEntities.STALKER.get().create(serverLevel);

        if (stalker == null) {
            return false;
        }

        RandomSource random = serverLevel.getRandom();

        stalker.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                random.nextFloat() * 360.0F,
                0.0F
        );

        if (!serverLevel.noCollision(stalker)) {
            return false;
        }

        stalker.finalizeSpawn(
                serverLevel,
                serverLevel.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.EVENT,
                null,
                null
        );

        serverLevel.addFreshEntity(stalker);

        return true;
    }
}
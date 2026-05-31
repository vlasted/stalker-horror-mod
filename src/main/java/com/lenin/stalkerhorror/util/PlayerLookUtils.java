package com.lenin.stalkerhorror.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PlayerLookUtils {
    public static boolean isPlayerLookingAtEntity(Player player, Entity entity, double maxDistance, double dotThreshold) {
        if (player.distanceTo(entity) > maxDistance) {
            return false;
        }

        Vec3 playerView = player.getViewVector(1.0F).normalize();
        Vec3 entityPosition = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Vec3 directionToEntity = entityPosition.subtract(player.getEyePosition()).normalize();

        double dot = playerView.dot(directionToEntity);

        return dot > dotThreshold && player.hasLineOfSight(entity);
    }
}
package com.lenin.stalkerhorror.registry;

import com.lenin.stalkerhorror.StalkerHorrorMod;
import com.lenin.stalkerhorror.entity.StalkerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StalkerHorrorMod.MODID);

    public static final RegistryObject<EntityType<StalkerEntity>> STALKER =
            ENTITY_TYPES.register("stalker", () ->
                    EntityType.Builder.of(StalkerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build(StalkerHorrorMod.MODID + ":stalker")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
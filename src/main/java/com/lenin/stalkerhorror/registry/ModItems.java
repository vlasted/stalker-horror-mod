package com.lenin.stalkerhorror.registry;

import com.lenin.stalkerhorror.StalkerHorrorMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, StalkerHorrorMod.MODID);

    public static final RegistryObject<Item> STALKER_SPAWN_EGG =
            ITEMS.register("stalker_spawn_egg", () ->
                    new ForgeSpawnEggItem(
                            ModEntities.STALKER,
                            0x111111,
                            0xd8d0b8,
                            new Item.Properties()
                    )
            );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(STALKER_SPAWN_EGG.get());
        }
    }
}
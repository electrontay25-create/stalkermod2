package com.stalkermod.init;

import com.stalkermod.StalkerMod;
import com.stalkermod.item.StalkerWatchItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, StalkerMod.MODID);

    /** Huevo generador opcional (solo para pruebas en creativo). */
    public static final RegistryObject<Item> STALKER_SPAWN_EGG = ITEMS.register("stalker_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.STALKER, 0x070709, 0xC4BFAB, new Item.Properties()));

    /** Reloj del Stalker: mantenlo presionado (clic derecho) para abrir los ajustes. */
    public static final RegistryObject<Item> STALKER_WATCH = ITEMS.register("stalker_watch",
            () -> new StalkerWatchItem(new Item.Properties().stacksTo(1)));
}

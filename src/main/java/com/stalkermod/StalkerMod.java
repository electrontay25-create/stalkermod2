package com.stalkermod;

import com.stalkermod.init.ModEntities;
import com.stalkermod.init.ModItems;
import com.stalkermod.init.ModSounds;
import com.stalkermod.init.StalkerConfig;
import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.network.ModNetwork;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Clase principal del mod. Registra entidad, sonidos, items, red y configuracion.
 */
@Mod(StalkerMod.MODID)
public class StalkerMod {

    public static final String MODID = "stalkermod";

    public StalkerMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModItems.ITEMS.register(modBus);

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onAttributes);
        modBus.addListener(this::onCreativeTab);

        // Archivo de configuracion: config/stalkermod-common.toml
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, StalkerConfig.SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    /** Todo Mob necesita atributos registrados. */
    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.STALKER.get(), StalkerEntity.createAttributes().build());
    }

    /** Huevo en "Spawn Eggs" y el Reloj en "Herramientas y utilidades". */
    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.STALKER_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.STALKER_WATCH);
        }
    }
}

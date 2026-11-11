package com.stalkermod;

import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.ModEntities;
import com.stalkermod.init.ModItems;
import com.stalkermod.init.ModSounds;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Clase principal del mod. Registra entidad, sonidos, item (spawn egg) y configuracion.
 */
@Mod(StalkerMod.MODID)
public class StalkerMod {

    public static final String MODID = "stalkermod";

    public StalkerMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModItems.ITEMS.register(modBus);

        modBus.addListener(this::onAttributes);
        modBus.addListener(this::onCreativeTab);

        // Archivo de configuracion: config/stalkermod-common.toml
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, StalkerConfig.SPEC);
    }

    /** Todo Mob necesita atributos registrados. */
    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.STALKER.get(), StalkerEntity.createAttributes().build());
    }

    /** Pone el huevo generador en la pestana "Spawn Eggs" del creativo. */
    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.STALKER_SPAWN_EGG);
        }
    }
}

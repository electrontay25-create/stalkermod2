package com.stalkermod.init;

import com.stalkermod.StalkerMod;
import com.stalkermod.entity.StalkerEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StalkerMod.MODID);

    /** MISC = nunca aparece solo por el sistema de spawn de Minecraft; solo lo invoca el mod. */
    public static final RegistryObject<EntityType<StalkerEntity>> STALKER = ENTITIES.register("stalker",
            () -> EntityType.Builder.<StalkerEntity>of(StalkerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 2.9F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .build(new ResourceLocation(StalkerMod.MODID, "stalker").toString()));
}

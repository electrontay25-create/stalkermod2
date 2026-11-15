package com.stalkermod.network;

import com.stalkermod.StalkerMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red: servidor -> cliente (abrir ajustes) y cliente -> servidor (guardar ajustes). */
public class ModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StalkerMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenSettingsPacket.class,
                OpenSettingsPacket::encode, OpenSettingsPacket::decode, OpenSettingsPacket::handle);
        CHANNEL.registerMessage(id++, SaveSettingsPacket.class,
                SaveSettingsPacket::encode, SaveSettingsPacket::decode, SaveSettingsPacket::handle);
    }
}

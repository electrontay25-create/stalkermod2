package com.stalkermod.network;

import com.stalkermod.client.StalkerClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: "abre la ventana de ajustes con estos valores". */
public class OpenSettingsPacket {

    public final int minutes;
    public final int volume;
    public final int notice;

    public OpenSettingsPacket(int minutes, int volume, int notice) {
        this.minutes = minutes;
        this.volume = volume;
        this.notice = notice;
    }

    public static void encode(OpenSettingsPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.minutes);
        buf.writeInt(p.volume);
        buf.writeInt(p.notice);
    }

    public static OpenSettingsPacket decode(FriendlyByteBuf buf) {
        return new OpenSettingsPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(OpenSettingsPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        StalkerClientHooks.openSettings(p.minutes, p.volume, p.notice)));
        ctx.get().setPacketHandled(true);
    }
}

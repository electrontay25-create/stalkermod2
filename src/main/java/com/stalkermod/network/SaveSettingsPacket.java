package com.stalkermod.network;

import com.stalkermod.event.StalkerEvents;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: guarda los ajustes elegidos en la ventana. */
public class SaveSettingsPacket {

    public final int minutes;
    public final int volume;
    public final int notice;

    public SaveSettingsPacket(int minutes, int volume, int notice) {
        this.minutes = minutes;
        this.volume = volume;
        this.notice = notice;
    }

    public static void encode(SaveSettingsPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.minutes);
        buf.writeInt(p.volume);
        buf.writeInt(p.notice);
    }

    public static SaveSettingsPacket decode(FriendlyByteBuf buf) {
        return new SaveSettingsPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(SaveSettingsPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) {
                return;
            }
            // Un jugador solo puede cambiar ajustes si es operador o si esta en un mundo de un jugador.
            boolean allowed = sender.hasPermissions(2)
                    || (sender.getServer() != null && sender.getServer().isSingleplayer());
            if (!allowed) {
                sender.sendSystemMessage(Component.literal("Stalker: no tienes permiso para cambiar los ajustes."));
                return;
            }
            int minutes = Mth.clamp(p.minutes, 1, 240);
            int volume = Mth.clamp(p.volume, 0, 100);
            int notice = Mth.clamp(p.notice, 1, 30);

            StalkerConfig.STALK_INTERVAL_MINUTES.set(minutes);
            StalkerConfig.AMBIENT_VOLUME_PERCENT.set(volume);
            StalkerConfig.STALK_NOTICE_SECONDS.set(notice);
            StalkerConfig.SPEC.save();

            StalkerEvents.reschedule(sender);
            sender.sendSystemMessage(Component.literal("Stalker: guardado. Aparecera cada ~" + minutes + " min."));
        });
        ctx.get().setPacketHandled(true);
    }
}

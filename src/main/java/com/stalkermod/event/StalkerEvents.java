package com.stalkermod.event;

import com.stalkermod.StalkerMod;
import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reloj de los eventos aleatorios. Cada segundo, por jugador, comprueba el cooldown y tira un dado.
 * Tambien avanza el evento de puerta activo (si lo hay).
 */
@Mod.EventBusSubscriber(modid = StalkerMod.MODID)
public class StalkerEvents {

    private static class PlayerData {
        boolean initialized = false;
        long nextStalk = 0L;
        long nextDoor = 0L;
        DoorEvent door = null;
    }

    private static final Map<UUID, PlayerData> DATA = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        PlayerData data = DATA.computeIfAbsent(player.getUUID(), id -> new PlayerData());
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        // Avanzar el evento de puerta activo.
        if (data.door != null) {
            if (data.door.tick()) {
                data.door = null;
                data.nextDoor = now + StalkerConfig.DOOR_COOLDOWN_SECONDS.get() * 20L;
            }
            return;
        }

        if (now % 20L != 0L || player.isSpectator()) {
            return; // una vez por segundo
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        // Al entrar al mundo no pasa nada durante un rato.
        if (!data.initialized) {
            data.initialized = true;
            data.nextStalk = now + 60L * 20L;
            data.nextDoor = now + 90L * 20L;
            return;
        }

        // No dos stalkers a la vez cerca del jugador.
        boolean stalkerNearby = !level.getEntitiesOfClass(StalkerEntity.class,
                player.getBoundingBox().inflate(100.0D)).isEmpty();
        if (stalkerNearby) {
            return;
        }

        if (StalkerConfig.ENABLE_STALK.get() && now >= data.nextStalk
                && level.random.nextDouble() < StalkerConfig.STALK_CHANCE_PER_SECOND.get()) {
            if (StalkSpawner.trySpawn(player, false)) {
                data.nextStalk = now + StalkerConfig.STALK_COOLDOWN_SECONDS.get() * 20L;
                return;
            }
        }

        if (StalkerConfig.ENABLE_DOOR.get() && now >= data.nextDoor
                && level.random.nextDouble() < StalkerConfig.DOOR_CHANCE_PER_SECOND.get()) {
            DoorEvent ev = DoorEvent.tryCreate(player, false);
            if (ev != null) {
                data.door = ev;
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerData data = DATA.remove(event.getEntity().getUUID());
        if (data != null && data.door != null) {
            data.door.abort();
        }
    }

    // ---- usado por los comandos ----

    /** Inicia el evento de puerta de forma manual. Devuelve false si ya hay uno activo. */
    public static boolean startDoor(ServerPlayer player, DoorEvent event) {
        PlayerData data = DATA.computeIfAbsent(player.getUUID(), id -> new PlayerData());
        if (data.door != null) {
            return false;
        }
        data.door = event;
        return true;
    }

    /** Cancela eventos activos y elimina todos los Stalkers del mundo del jugador. */
    public static int clearAll(ServerPlayer player) {
        PlayerData data = DATA.get(player.getUUID());
        if (data != null && data.door != null) {
            data.door.abort();
            data.door = null;
        }
        ServerLevel level = (ServerLevel) player.level();
        int removed = 0;
        for (StalkerEntity e : level.getEntitiesOfClass(StalkerEntity.class,
                player.getBoundingBox().inflate(300.0D))) {
            e.discard();
            removed++;
        }
        return removed;
    }
}

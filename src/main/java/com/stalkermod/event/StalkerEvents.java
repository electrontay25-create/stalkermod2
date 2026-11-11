package com.stalkermod.event;

import com.stalkermod.StalkerMod;
import com.stalkermod.entity.StalkerEntity;
import com.stalkermod.init.ModItems;
import com.stalkermod.init.StalkerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reloj de los eventos automaticos.
 *
 * Acechar: cada X minutos (config, por defecto 7, +-20 %) el Stalker intenta aparecer detras de algo.
 *   Si en ese momento no hay donde esconderse, reintenta cada 2 segundos hasta que el jugador pase
 *   cerca de arboles/paredes, asi que siempre aparece cuando hay un buen lugar.
 * Puerta: cada Y minutos, solo si estas dentro de una casa de noche con puerta + ventana.
 */
@Mod.EventBusSubscriber(modid = StalkerMod.MODID)
public class StalkerEvents {

    private static class PlayerData {
        long nextStalk = -1L;
        long nextDoor = -1L;
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
                data.nextDoor = now + doorDelay(level.random);
                data.nextStalk = Math.max(data.nextStalk, now + 60L * 20L);
            }
            return;
        }

        if (now % 20L != 0L || player.isSpectator()) {
            return; // una vez por segundo
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        // Primera vez: empieza a contar desde que entras al mundo.
        if (data.nextStalk < 0L) {
            data.nextStalk = now + stalkDelay(level.random);
            data.nextDoor = now + doorDelay(level.random);
            return;
        }

        // No dos Stalkers a la vez cerca del jugador.
        boolean stalkerNearby = !level.getEntitiesOfClass(StalkerEntity.class,
                player.getBoundingBox().inflate(100.0D)).isEmpty();
        if (stalkerNearby) {
            return;
        }

        // Acechar (se intenta en los segundos "pares").
        if (StalkerConfig.ENABLE_STALK.get() && now >= data.nextStalk && now % 40L == 0L) {
            if (StalkSpawner.trySpawn(player, false)) {
                data.nextStalk = now + stalkDelay(level.random);
                data.nextDoor = Math.max(data.nextDoor, now + 60L * 20L);
                return;
            }
        }

        // Puerta (se intenta en los segundos "impares").
        if (StalkerConfig.ENABLE_DOOR.get() && now >= data.nextDoor && now % 40L == 20L) {
            DoorEvent ev = DoorEvent.tryCreate(player, false);
            if (ev != null) {
                data.door = ev;
            }
        }
    }

    private static long stalkDelay(RandomSource rnd) {
        return jitter(StalkerConfig.STALK_INTERVAL_MINUTES.get() * 60L * 20L, rnd);
    }

    private static long doorDelay(RandomSource rnd) {
        return jitter(StalkerConfig.DOOR_INTERVAL_MINUTES.get() * 60L * 20L, rnd);
    }

    /** +-20 % para que no sea predecible. */
    private static long jitter(long base, RandomSource rnd) {
        double f = 1.0D + (rnd.nextDouble() * 2.0D - 1.0D) * 0.20D;
        return (long) (base * f);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerData data = DATA.remove(event.getEntity().getUUID());
        if (data != null && data.door != null) {
            data.door.abort();
        }
    }

    /** La primera vez que entras al mundo recibes el Reloj del Stalker (una sola vez). */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound("PlayerPersisted"); // se conserva al morir
        if (!persisted.getBoolean("stalkermod_got_watch")) {
            giveWatch(player);
            persisted.putBoolean("stalkermod_got_watch", true);
            root.put("PlayerPersisted", persisted);
        }
    }

    public static void giveWatch(ServerPlayer player) {
        ItemStack stack = new ItemStack(ModItems.STALKER_WATCH.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ---- usado por los comandos / ajustes ----

    /** Vuelve a programar la proxima aparicion con el intervalo actual (despues de cambiar ajustes). */
    public static void reschedule(ServerPlayer player) {
        PlayerData data = DATA.computeIfAbsent(player.getUUID(), id -> new PlayerData());
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        data.nextStalk = now + stalkDelay(level.random);
    }

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

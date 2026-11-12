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
import java.util.function.BooleanSupplier;

/**
 * Reloj de los eventos automaticos. Hay tres, cada uno con su propio temporizador:
 *   - ACECHAR (cuerpo asomado)  : cada ~7 min, 70 % de probabilidad.
 *   - MEDIA CABEZA              : cada ~10 min, 50 % de probabilidad.
 *   - PUERTA                    : cada ~10 min, 60 % de probabilidad (de dia o de noche).
 *
 * Cuando "le toca" y la probabilidad sale bien, el evento queda PENDIENTE: se reintenta cada 2 segundos
 * hasta que haya un lugar valido (arboles, paredes, una puerta...) o pasen 3 minutos.
 * Despues de un evento de puerta hay un 20 % de que enseguida venga otro evento.
 */
@Mod.EventBusSubscriber(modid = StalkerMod.MODID)
public class StalkerEvents {

    private static final long PENDING_TICKS = 3L * 60L * 20L;

    private static class Timer {
        long next = -1L;
        long pendingUntil = 0L;
        boolean force = false;   // true = saltarse la probabilidad (evento encadenado)
    }

    private static class PlayerData {
        final Timer stalk = new Timer();
        final Timer watch = new Timer();
        final Timer door = new Timer();
        DoorEvent doorEvent = null;
        long gapUntil = 0L;
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
        RandomSource rnd = level.random;

        // Avanzar el evento de puerta activo.
        if (data.doorEvent != null) {
            if (data.doorEvent.tick()) {
                data.doorEvent = null;
                data.gapUntil = now + 25L * 20L;
                data.door.next = now + delay(StalkerConfig.DOOR_INTERVAL_MINUTES.get(), rnd);
                chainAfterDoor(data, now, rnd);
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
        if (data.stalk.next < 0L) {
            data.stalk.next = now + delay(StalkerConfig.STALK_INTERVAL_MINUTES.get(), rnd);
            data.watch.next = now + delay(StalkerConfig.WATCH_INTERVAL_MINUTES.get(), rnd);
            data.door.next = now + delay(StalkerConfig.DOOR_INTERVAL_MINUTES.get(), rnd);
            return;
        }

        // Un solo evento a la vez, con un respiro entre uno y otro.
        boolean stalkerNearby = !level.getEntitiesOfClass(StalkerEntity.class,
                player.getBoundingBox().inflate(100.0D)).isEmpty();
        if (stalkerNearby) {
            data.gapUntil = now + 25L * 20L;
            return;
        }
        if (now < data.gapUntil) {
            return;
        }

        boolean attemptTick = (now % 40L == 0L);   // los intentos pesados, cada 2 segundos

        if (StalkerConfig.ENABLE_STALK.get()
                && process(data.stalk, now, rnd, StalkerConfig.STALK_CHANCE_PERCENT.get(),
                StalkerConfig.STALK_INTERVAL_MINUTES.get(), attemptTick,
                () -> StalkSpawner.trySpawn(player, false))) {
            return;
        }
        if (StalkerConfig.ENABLE_WATCH.get()
                && process(data.watch, now, rnd, StalkerConfig.WATCH_CHANCE_PERCENT.get(),
                StalkerConfig.WATCH_INTERVAL_MINUTES.get(), attemptTick,
                () -> WatchSpawner.trySpawn(player, false, 150))) {
            return;
        }
        if (StalkerConfig.ENABLE_DOOR.get()
                && process(data.door, now, rnd, StalkerConfig.DOOR_CHANCE_PERCENT.get(),
                StalkerConfig.DOOR_INTERVAL_MINUTES.get(), attemptTick, () -> {
                    DoorEvent ev = DoorEvent.tryCreate(player, false);
                    if (ev == null) {
                        return false;
                    }
                    data.doorEvent = ev;
                    return true;
                })) {
            return;
        }
    }

    /**
     * Avanza un temporizador. @return true si en este instante el evento se acaba de lanzar.
     */
    private static boolean process(Timer t, long now, RandomSource rnd, int chancePercent, int intervalMinutes,
                                   boolean attemptTick, BooleanSupplier attempt) {
        if (t.pendingUntil == 0L && now >= t.next) {
            if (t.force || rnd.nextInt(100) < chancePercent) {
                t.force = false;
                t.pendingUntil = now + PENDING_TICKS;
            } else {
                t.next = now + delay(intervalMinutes, rnd);   // esta vez no toca: espera al siguiente turno
            }
        }
        if (t.pendingUntil != 0L) {
            if (attemptTick && attempt.getAsBoolean()) {
                t.pendingUntil = 0L;
                t.next = now + delay(intervalMinutes, rnd);
                return true;
            }
            if (now > t.pendingUntil) {
                t.pendingUntil = 0L;                           // no hubo lugar valido en 3 minutos
                t.next = now + delay(intervalMinutes, rnd);
            }
        }
        return false;
    }

    /** Despues de la puerta: probabilidad de que enseguida venga otro evento. */
    private static void chainAfterDoor(PlayerData data, long now, RandomSource rnd) {
        if (rnd.nextInt(100) >= StalkerConfig.CHAIN_CHANCE_PERCENT.get()) {
            return;
        }
        int pick = rnd.nextInt(3);
        Timer t = (pick == 0) ? data.stalk : (pick == 1) ? data.watch : data.door;
        boolean enabled = (pick == 0) ? StalkerConfig.ENABLE_STALK.get()
                : (pick == 1) ? StalkerConfig.ENABLE_WATCH.get() : StalkerConfig.ENABLE_DOOR.get();
        if (!enabled) {
            return;
        }
        t.next = now + 30L * 20L + rnd.nextInt(60 * 20);       // en 30-90 segundos
        t.force = true;
        t.pendingUntil = 0L;
    }

    private static long delay(int minutes, RandomSource rnd) {
        double f = 1.0D + (rnd.nextDouble() * 2.0D - 1.0D) * 0.20D;  // +-20 %
        return (long) (minutes * 60L * 20L * f);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerData data = DATA.remove(event.getEntity().getUUID());
        if (data != null && data.doorEvent != null) {
            data.doorEvent.abort();
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
        data.stalk.next = now + delay(StalkerConfig.STALK_INTERVAL_MINUTES.get(), level.random);
        data.stalk.pendingUntil = 0L;
    }

    /** Inicia el evento de puerta de forma manual. Devuelve false si ya hay uno activo. */
    public static boolean startDoor(ServerPlayer player, DoorEvent event) {
        PlayerData data = DATA.computeIfAbsent(player.getUUID(), id -> new PlayerData());
        if (data.doorEvent != null) {
            return false;
        }
        data.doorEvent = event;
        return true;
    }

    /** Cancela eventos activos y elimina todos los Stalkers del mundo del jugador. */
    public static int clearAll(ServerPlayer player) {
        PlayerData data = DATA.get(player.getUUID());
        if (data != null && data.doorEvent != null) {
            data.doorEvent.abort();
            data.doorEvent = null;
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

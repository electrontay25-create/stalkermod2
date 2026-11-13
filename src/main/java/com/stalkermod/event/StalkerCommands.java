package com.stalkermod.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.stalkermod.StalkerMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Comandos de prueba (requieren permisos de operador / trucos activados):
 *   /stalker stalk  -> fuerza el evento "acechar" (cuerpo asomado) ahora mismo
 *   /stalker watch  -> fuerza el evento "asomado en pasillos/cuevas" ahora mismo
 *   /stalker status -> muestra cuanto falta para cada evento automatico
 *   /stalker door   -> fuerza el evento "puerta"
 *   /stalker item   -> te da el Reloj del Stalker (ajustes)
 *   /stalker clear  -> elimina todos los Stalkers y cancela el evento de puerta
 */
@Mod.EventBusSubscriber(modid = StalkerMod.MODID)
public class StalkerCommands {

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("stalker")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("stalk").executes(ctx -> stalk(ctx.getSource())))
                .then(Commands.literal("watch").executes(ctx -> watch(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("door").executes(ctx -> door(ctx.getSource())))
                .then(Commands.literal("item").executes(ctx -> item(ctx.getSource())))
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource()))));
    }

    private static int stalk(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        // Primero intenta aparecer fuera de tu vista (lo normal); si no se puede, lo permite en cualquier lado.
        boolean ok = StalkSpawner.trySpawn(player, false) || StalkSpawner.trySpawn(player, true);
        if (ok) {
            src.sendSuccess(() -> Component.literal("Stalker: aparecio detras de algo. Date la vuelta..."), false);
            return 1;
        }
        src.sendFailure(Component.literal("No encontre donde esconderse en 12-26 bloques. Acercate a arboles, paredes o una cueva e intenta de nuevo."));
        return 0;
    }

    private static int watch(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        boolean ok = StalkSpawner.trySpawn(player, false, true) || StalkSpawner.trySpawn(player, true, true);
        if (ok) {
            src.sendSuccess(() -> Component.literal("Stalker: algo se asoma por una esquina. Mira a tu alrededor..."), false);
            return 1;
        }
        src.sendFailure(Component.literal("No encontre una esquina/muro donde asomarse a 5-18 bloques (se necesitan 2+ bloques de alto con espacio libre al lado). Ve a una cueva, pasillo o casa e intenta de nuevo."));
        return 0;
    }

    private static int door(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        DoorEvent ev = DoorEvent.tryCreate(player, true);
        if (ev == null) {
            src.sendFailure(Component.literal("No encontre una puerta de madera cerrada cerca de ti. Acercate a una."));
            return 0;
        }
        if (!StalkerEvents.startDoor(player, ev)) {
            src.sendFailure(Component.literal("Ya hay un evento de puerta en curso. Usa /stalker clear."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Stalker: alguien toca la puerta..."), false);
        return 1;
    }

    private static int status(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        String text = StalkerEvents.status(player);
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int item(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        StalkerEvents.giveWatch(player);
        src.sendSuccess(() -> Component.literal("Reloj del Stalker entregado. Mantenlo presionado (clic derecho) para abrir los ajustes."), false);
        return 1;
    }

    private static int clear(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        int n = StalkerEvents.clearAll(player);
        src.sendSuccess(() -> Component.literal("Stalkers eliminados: " + n), false);
        return n;
    }
}

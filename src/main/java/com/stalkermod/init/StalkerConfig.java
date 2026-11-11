package com.stalkermod.init;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuracion (archivo config/stalkermod-common.toml).
 * Casi todo se puede cambiar tambien dentro del juego con el Reloj del Stalker (mantenlo presionado).
 */
public class StalkerConfig {

    public static final ForgeConfigSpec SPEC;

    // ---- General ----
    public static final ForgeConfigSpec.BooleanValue ENABLE_STALK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DOOR;

    // ---- Evento 1: acechar ----
    public static final ForgeConfigSpec.IntValue STALK_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.IntValue STALK_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue STALK_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue STALK_APPROACH_DISTANCE;
    public static final ForgeConfigSpec.IntValue STALK_NOTICE_SECONDS;
    public static final ForgeConfigSpec.IntValue STALK_MAX_LIFE_SECONDS;
    public static final ForgeConfigSpec.IntValue AMBIENT_VOLUME_PERCENT;

    // ---- Evento 2: puerta ----
    public static final ForgeConfigSpec.IntValue DOOR_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.BooleanValue DOOR_ONLY_AT_NIGHT;
    public static final ForgeConfigSpec.IntValue DOOR_SEARCH_RADIUS;
    public static final ForgeConfigSpec.IntValue BLINDNESS_SECONDS;
    public static final ForgeConfigSpec.IntValue BLINDNESS_AMPLIFIER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        ENABLE_STALK = b.comment("Activa el evento 'acechar' (aparece detras de arboles/paredes).")
                .define("enableStalk", true);
        ENABLE_DOOR = b.comment("Activa el evento 'tocar la puerta'.")
                .define("enableDoor", true);
        b.pop();

        b.push("stalk");
        STALK_INTERVAL_MINUTES = b.comment("Cada cuantos minutos aparece el Stalker (con una variacion de +-20%).")
                .defineInRange("intervalMinutes", 7, 1, 240);
        STALK_MIN_DISTANCE = b.comment("Distancia minima (bloques) a la que aparece.")
                .defineInRange("minDistance", 12, 6, 64);
        STALK_MAX_DISTANCE = b.comment("Distancia maxima (bloques) a la que aparece.")
                .defineInRange("maxDistance", 26, 8, 96);
        STALK_APPROACH_DISTANCE = b.comment("Si te acercas a menos de esta distancia, se esconde rapido.")
                .defineInRange("approachDistance", 8, 2, 48);
        STALK_NOTICE_SECONDS = b.comment("Segundos (aprox.) que puedes mirarlo antes de que se de cuenta y se esconda.")
                .defineInRange("noticeSeconds", 4, 1, 30);
        STALK_MAX_LIFE_SECONDS = b.comment("Tiempo maximo que se queda aunque no lo mires.")
                .defineInRange("maxLifeSeconds", 45, 5, 300);
        AMBIENT_VOLUME_PERCENT = b.comment("Volumen de la musica de terror (0 = sin musica, solo el ruido de hojas).")
                .defineInRange("ambientVolumePercent", 35, 0, 100);
        b.pop();

        b.push("door");
        DOOR_INTERVAL_MINUTES = b.comment("Cada cuantos minutos puede ocurrir el evento de la puerta (si hay condiciones).")
                .defineInRange("intervalMinutes", 12, 1, 240);
        DOOR_ONLY_AT_NIGHT = b.comment("Si es true, el evento aleatorio solo ocurre de noche (el comando /stalker door ignora esto).")
                .define("onlyAtNight", true);
        DOOR_SEARCH_RADIUS = b.comment("Radio (bloques) para buscar una puerta de madera con ventana al lado.")
                .defineInRange("searchRadius", 8, 3, 16);
        BLINDNESS_SECONDS = b.comment("Duracion de la ceguera al romper la puerta.")
                .defineInRange("blindnessSeconds", 10, 1, 120);
        BLINDNESS_AMPLIFIER = b.comment("Nivel de ceguera menos 1 (2 = ceguera III).")
                .defineInRange("blindnessAmplifier", 2, 0, 10);
        b.pop();

        SPEC = b.build();
    }
}

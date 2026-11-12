package com.stalkermod.init;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuracion (archivo config/stalkermod-common.toml).
 * Los ajustes principales tambien se cambian dentro del juego con el Reloj del Stalker.
 */
public class StalkerConfig {

    public static final ForgeConfigSpec SPEC;

    // ---- General ----
    public static final ForgeConfigSpec.BooleanValue ENABLE_STALK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WATCH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DOOR;
    public static final ForgeConfigSpec.IntValue CHAIN_CHANCE_PERCENT;
    public static final ForgeConfigSpec.IntValue AMBIENT_VOLUME_PERCENT;

    // ---- Evento 1: acechar (cuerpo asomado detras de algo) ----
    public static final ForgeConfigSpec.IntValue STALK_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.IntValue STALK_CHANCE_PERCENT;
    public static final ForgeConfigSpec.IntValue STALK_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue STALK_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue STALK_APPROACH_DISTANCE;
    public static final ForgeConfigSpec.IntValue STALK_NOTICE_SECONDS;
    public static final ForgeConfigSpec.IntValue STALK_MAX_LIFE_SECONDS;

    // ---- Evento 2: media cabeza ----
    public static final ForgeConfigSpec.IntValue WATCH_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.IntValue WATCH_CHANCE_PERCENT;
    public static final ForgeConfigSpec.IntValue WATCH_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue WATCH_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue WATCH_STARE_SECONDS;

    // ---- Evento 3: puerta ----
    public static final ForgeConfigSpec.IntValue DOOR_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.IntValue DOOR_CHANCE_PERCENT;
    public static final ForgeConfigSpec.BooleanValue DOOR_ONLY_AT_NIGHT;
    public static final ForgeConfigSpec.IntValue DOOR_SEARCH_RADIUS;
    public static final ForgeConfigSpec.IntValue BLINDNESS_SECONDS;
    public static final ForgeConfigSpec.IntValue BLINDNESS_AMPLIFIER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("general");
        ENABLE_STALK = b.comment("Activa el evento 'acechar' (cuerpo asomado detras de un arbol/pared).")
                .define("enableStalk", true);
        ENABLE_WATCH = b.comment("Activa el evento 'media cabeza' (te observa desde detras de un muro/esquina).")
                .define("enableWatch", true);
        ENABLE_DOOR = b.comment("Activa el evento 'tocar la puerta'.")
                .define("enableDoor", true);
        CHAIN_CHANCE_PERCENT = b.comment("Despues de un evento de puerta, probabilidad (%) de que venga otro evento enseguida.")
                .defineInRange("chainChancePercent", 20, 0, 100);
        AMBIENT_VOLUME_PERCENT = b.comment("Volumen de la musica/chillido de terror (0 = sin musica).")
                .defineInRange("ambientVolumePercent", 35, 0, 100);
        b.pop();

        b.push("stalk");
        STALK_INTERVAL_MINUTES = b.comment("Cada cuantos minutos se intenta el evento (con variacion de +-20%).")
                .defineInRange("intervalMinutes", 7, 1, 240);
        STALK_CHANCE_PERCENT = b.comment("Probabilidad (%) de que el evento ocurra cuando le toca.")
                .defineInRange("chancePercent", 70, 0, 100);
        STALK_MIN_DISTANCE = b.comment("Distancia minima (bloques) a la que aparece.")
                .defineInRange("minDistance", 12, 6, 64);
        STALK_MAX_DISTANCE = b.comment("Distancia maxima (bloques) a la que aparece.")
                .defineInRange("maxDistance", 26, 8, 96);
        STALK_APPROACH_DISTANCE = b.comment("Si te acercas a menos de esta distancia, se esconde rapido.")
                .defineInRange("approachDistance", 8, 2, 48);
        STALK_NOTICE_SECONDS = b.comment("Segundos (aprox.) que puedes mirarlo antes de que se esconda detras de la cobertura.")
                .defineInRange("noticeSeconds", 4, 1, 30);
        STALK_MAX_LIFE_SECONDS = b.comment("Tiempo maximo que se queda aunque no lo mires.")
                .defineInRange("maxLifeSeconds", 45, 5, 300);
        b.pop();

        b.push("watch");
        WATCH_INTERVAL_MINUTES = b.comment("Cada cuantos minutos se intenta el evento (con variacion de +-20%).")
                .defineInRange("intervalMinutes", 10, 1, 240);
        WATCH_CHANCE_PERCENT = b.comment("Probabilidad (%) de que el evento ocurra cuando le toca.")
                .defineInRange("chancePercent", 50, 0, 100);
        WATCH_MIN_DISTANCE = b.comment("Distancia minima (bloques) de la media cabeza.")
                .defineInRange("minDistance", 5, 3, 40);
        WATCH_MAX_DISTANCE = b.comment("Distancia maxima (bloques) de la media cabeza.")
                .defineInRange("maxDistance", 18, 6, 40);
        WATCH_STARE_SECONDS = b.comment("Segundos que puedes mirarla antes de que desaparezca lentamente.")
                .defineInRange("stareSeconds", 8, 1, 60);
        b.pop();

        b.push("door");
        DOOR_INTERVAL_MINUTES = b.comment("Cada cuantos minutos se intenta el evento (con variacion de +-20%).")
                .defineInRange("intervalMinutes", 10, 1, 240);
        DOOR_CHANCE_PERCENT = b.comment("Probabilidad (%) de que el evento ocurra cuando le toca.")
                .defineInRange("chancePercent", 60, 0, 100);
        DOOR_ONLY_AT_NIGHT = b.comment("true = solo de noche. false = de dia o de noche.")
                .define("onlyAtNight", false);
        DOOR_SEARCH_RADIUS = b.comment("Radio (bloques) para buscar una puerta de madera cerca de ti.")
                .defineInRange("searchRadius", 10, 3, 16);
        BLINDNESS_SECONDS = b.comment("Duracion de la ceguera al romper la puerta.")
                .defineInRange("blindnessSeconds", 10, 1, 120);
        BLINDNESS_AMPLIFIER = b.comment("Nivel de ceguera menos 1 (2 = ceguera III).")
                .defineInRange("blindnessAmplifier", 2, 0, 10);
        b.pop();

        SPEC = b.build();
    }
}

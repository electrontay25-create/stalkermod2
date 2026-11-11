package com.stalkermod.init;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Configuracion (archivo config/stalkermod-common.toml). Todos los tiempos estan en SEGUNDOS.
 */
public class StalkerConfig {

    public static final ForgeConfigSpec SPEC;

    // ---- General ----
    public static final ForgeConfigSpec.BooleanValue ENABLE_STALK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DOOR;

    // ---- Evento 1: acechar ----
    public static final ForgeConfigSpec.IntValue STALK_MIN_DISTANCE;
    public static final ForgeConfigSpec.IntValue STALK_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue STALK_APPROACH_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue STALK_FADE_SECONDS;
    public static final ForgeConfigSpec.IntValue STALK_STARE_SECONDS;
    public static final ForgeConfigSpec.IntValue STALK_MAX_LIFE_SECONDS;
    public static final ForgeConfigSpec.IntValue STALK_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue STALK_CHANCE_PER_SECOND;

    // ---- Evento 2: puerta ----
    public static final ForgeConfigSpec.IntValue DOOR_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DOOR_CHANCE_PER_SECOND;
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
        STALK_MIN_DISTANCE = b.comment("Distancia minima (bloques) a la que aparece.")
                .defineInRange("minDistance", 20, 8, 64);
        STALK_MAX_DISTANCE = b.comment("Distancia maxima (bloques) a la que aparece.")
                .defineInRange("maxDistance", 32, 10, 96);
        STALK_APPROACH_DISTANCE = b.comment("Si el jugador se acerca a menos de esta distancia, empieza a desaparecer.")
                .defineInRange("approachDistance", 15, 3, 48);
        STALK_FADE_SECONDS = b.comment("Segundos que tarda en desvanecerse (rapido pero no instantaneo).")
                .defineInRange("fadeSeconds", 1.8D, 0.3D, 10.0D);
        STALK_STARE_SECONDS = b.comment("Segundos mirandolo directamente antes de que desaparezca.")
                .defineInRange("stareSeconds", 6, 1, 60);
        STALK_MAX_LIFE_SECONDS = b.comment("Tiempo maximo que se queda aunque no lo mires.")
                .defineInRange("maxLifeSeconds", 45, 5, 300);
        STALK_COOLDOWN_SECONDS = b.comment("Espera minima entre apariciones (por jugador).")
                .defineInRange("cooldownSeconds", 300, 10, 7200);
        STALK_CHANCE_PER_SECOND = b.comment("Probabilidad por segundo (cuando el cooldown ya paso) de intentar la aparicion.")
                .defineInRange("chancePerSecond", 0.02D, 0.0D, 1.0D);
        b.pop();

        b.push("door");
        DOOR_COOLDOWN_SECONDS = b.comment("Espera minima entre eventos de puerta (por jugador).")
                .defineInRange("cooldownSeconds", 600, 10, 7200);
        DOOR_CHANCE_PER_SECOND = b.comment("Probabilidad por segundo (cuando el cooldown ya paso) de intentar el evento.")
                .defineInRange("chancePerSecond", 0.02D, 0.0D, 1.0D);
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

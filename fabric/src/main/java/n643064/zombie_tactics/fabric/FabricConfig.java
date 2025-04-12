package n643064.zombie_tactics.fabric;

import n643064.zombie_tactics.Config;

import eu.midnightdust.lib.config.MidnightConfig;


// I think MidnightConfig is even better than cloth-config :(
// Anyway, it causes the duplication of language assets
public class FabricConfig extends MidnightConfig {
    public static final String ANIMALS = "Animals";
    public static final String MINING = "Mining";
    public static final String CLIMBING = "Climbing";
    public static final String GENERAL = "General";
    public static final String SPAWN = "Spawn";

    // category shuffled because I sorted it to the types
    // which is better?
    @Entry(category = MINING) public static boolean do_mine = Config.mineBlocks;
    @Entry(category = ANIMALS) public static boolean do_hurt_animals = Config.targetAnimals;
    @Entry(category = GENERAL) public static boolean attack_invisible = Config.attackInvisible;
    @Entry(category = CLIMBING) public static boolean do_climb = Config.zombiesClimbing;
    @Entry(category = MINING) public static boolean drop_blocks = Config.dropBlocks;
    @Entry(category = GENERAL) public static boolean sun_sensitive = Config.sunSensitive;
    @Entry(category = GENERAL) public static boolean no_mercy = Config.noMercy;

    @Entry(category = MINING, min = 0) public static double mining_speed = Config.increment;
    @Entry(category = MINING, min = 0) public static double max_hardness = Config.maxHardness;
    @Entry(category = MINING, min = 0)  public static double hardness_multiplier = Config.hardnessMultiplier;
    @Entry(category = CLIMBING, min = 0) public static double climb_speed = Config.climbingSpeed;
    @Entry(category = MINING, min = 0) public static double min_mine_dist = Config.minDist;
    @Entry(category = MINING, min = 0) public static double max_mine_dist = Config.maxDist;
    @Entry(category = GENERAL, min = 0, max = 1024) public static double heal_amount = Config.healAmount;
    @Entry(category = GENERAL, min = 0.01, max = 128) public static double aggressive_speed = Config.aggressiveSpeed;
    @Entry(category = GENERAL, min = 0.25, max = 127) public static double attack_range = Config.attackRange;
    @Entry(category = SPAWN, min = 0, max = 1) public static double persistence_chance = Config.persistenceChance;

    @Entry(category = ANIMALS, min = 0) public static int hurt_animal_priority = Config.targetAnimalsPriority;
    @Entry(category = MINING, min = 0) public static int mine_priority = Config.miningPriority;
    @Entry(category = GENERAL, min = 1, max = 1000) public static int attack_cooldown = Config.attackCooldown;

    // fabric fields do nothing without the update of config
    public static void updateConfig() {
        Config.mineBlocks = do_mine;
        Config.targetAnimals = do_hurt_animals;
        Config.attackInvisible = attack_invisible;
        Config.zombiesClimbing = do_climb;
        Config.dropBlocks = drop_blocks;
        Config.sunSensitive = sun_sensitive;
        Config.noMercy = no_mercy;

        Config.increment = mining_speed;
        Config.maxHardness = max_hardness;
        Config.hardnessMultiplier = hardness_multiplier;
        Config.climbingSpeed = climb_speed;
        Config.minDist = min_mine_dist;
        Config.maxDist = max_mine_dist;
        Config.healAmount = heal_amount;
        Config.aggressiveSpeed = aggressive_speed;
        Config.attackRange = attack_range;
        Config.persistenceChance = persistence_chance;

        Config.targetAnimalsPriority = hurt_animal_priority;
        Config.miningPriority = mine_priority;
        Config.attackCooldown = attack_cooldown;
    }

    @Override
    public void writeChanges(String modid) {
        super.writeChanges(modid);
        updateConfig();
    }
}

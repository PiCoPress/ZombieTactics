package n643064.zombie_tactics.mixin;

import n643064.zombie_tactics.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Monster.class)
public abstract class MonsterMixin extends PathfinderMob {
    protected MonsterMixin(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    // zombie-like mobs can be spawned in sunny day
    @Inject(method="checkMonsterSpawnRules", at=@At(value="RETURN"), cancellable=true)
    private static void checkMonsterSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(level.getDifficulty() != Difficulty.PEACEFUL && (EntitySpawnReason.ignoresLightRequirements(spawnType) || Monster.isDarkEnoughToSpawn(level, pos, random) || (type == EntityType.ZOMBIE || type == EntityType.HUSK) && Config.spawnUnderSun) && checkMobSpawnRules(type, level, spawnType, pos, random));
    }
}

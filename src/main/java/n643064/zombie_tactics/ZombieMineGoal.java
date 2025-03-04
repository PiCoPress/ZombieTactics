package n643064.zombie_tactics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ZombieMineGoal<T extends Zombie> extends Goal
{
    final T zombie;
    final Level level;
    BlockPos target;
    double progress, hardness = Double.MAX_VALUE;
    boolean doMining;

    // These are constant unless a target is changed
    double X, Y, Z;

    /*  Y
        /\   Z
        |   /
        |  /
        | /
        |/________ X
        O
     */
    final byte[][] candidates_pos = new byte[][] {
            // Y = 1
            // The blocks in front, behind, to the left and the right of eye level
            {0, 1, 1},
            {-1, 1, 0},
            {1, 1, 0},
            {0, 1, -1},

            // Y = 0
            {0, 0, 1},
            {-1, 0, 0},
            {1, 0, 0},
            {0, 0, -1},

            // Y = 2
            {0, 2, 0},

            // Y = -1
            {0, -1, 0},
    };

    public ZombieMineGoal(T zombie)
    {
        doMining = true;
        this.zombie = zombie;
        level = zombie.level();
    }

    @Override
    public boolean requiresUpdateEveryTick()
    {
        return true;
    }

    @Override
    public void start() {
        doMining = true;
        progress = 0;
        hardness = level.getBlockState(target).getBlock().defaultDestroyTime() * Config.hardnessMult;
    }

    boolean checkBlock(BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        final Block b = state.getBlock();
        float destroying = b.defaultDestroyTime();
        //System.out.println("check " + pos);
        if(!b.isPossibleToRespawnInThis(state) &&
                destroying >= 0 &&
                destroying <= Config.maxHardness) // exclude unbreakable blocks
        {
            doMining = true;
            target = pos;
            X = pos.getX();
            Y = pos.getY();
            Z = pos.getZ();
            return true;
        }
        return false;
    }

    @Override
    public void stop()
    {
        zombie.level().destroyBlockProgress(zombie.getId(), target, -1);
        target = null;
        doMining = false;
        zombie.getNavigation().recomputePath();
        progress = 0;
        hardness = Double.MAX_VALUE;
    }

    @Override
    public void tick()
    {
        if (!doMining) return;
        if (zombie.distanceToSqr(X, Y, Z) <= Config.minDist ||
            zombie.distanceToSqr(X, Y, Z) > Config.maxDist)
        {
            doMining = false;
            return;
        }
        if(level.getBlockState(target).isAir())
        {
            // if the target has been broken by others
            level.destroyBlockProgress(zombie.getId(), target, -1);
            progress = 0;
            doMining = false;
            return;
        }
        if (progress >= hardness)
        {
            level.destroyBlock(target, Config.dropBlocks, zombie);
            zombie.level().destroyBlockProgress(zombie.getId(), target, -1);
            doMining = false;
        } else
        {
            level.destroyBlockProgress(zombie.getId(), target, (int) ((progress / hardness) * 10));
            zombie.stopInPlace();
            zombie.getLookControl().setLookAt(X, Y, Z);
            progress += Config.increment;
            zombie.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public boolean canContinueToUse()
    {
        return doMining && zombie.distanceToSqr(target.getCenter()) <= Config.maxDist;
    }

    @Override
    public boolean canUse()
    {
        // validate zombie position
        double x, y, z;
        x = zombie.getX();
        y = zombie.getY();
        z = zombie.getZ();
        if(X != x || Y != y || Z != z)
        {
            X = x; Y = y; Z = z;
            return false;
        }
        // check availability of the mining
        // found path but a zombie stuck
        LivingEntity liv = zombie.getTarget();
        PathNavigation nav = zombie.getNavigation();

        // I think that isStuck always return false
        if(zombie.isAlive() && !zombie.isNoAi() &&
                nav.isDone() && liv != null)
        {

            // once more
            boolean eval = nav.moveTo(liv, zombie.getSpeed());
            if(eval)
            {
                zombie.setTarget(liv);
                return false;
            }
            // ???
            if(zombie.distanceToSqr(liv) < 1.2) return false;
            for(byte[] pos: candidates_pos)
            {
                // checkBlock method is able to change 'zombie' variable
                // So 'temp' cannot be determined as valid object
                BlockPos temp = zombie.blockPosition().offset(pos[0], pos[1], pos[2]);
                if(checkBlock(temp))
                {
                    return true;
                }
            }
        }
        // zombie cannot escape
        return false;
    }
}

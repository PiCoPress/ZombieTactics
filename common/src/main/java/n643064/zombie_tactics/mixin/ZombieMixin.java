package n643064.zombie_tactics.mixin;

import n643064.zombie_tactics.Config;
import n643064.zombie_tactics.impl.NearestTargetGoal;
import n643064.zombie_tactics.mining.ZombieMineGoal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.NotNull;

//import java.util.Random;
import java.util.function.Predicate;


@Mixin(Zombie.class)
public abstract class ZombieMixin extends Monster {
    @Unique private ZombieMineGoal<? extends Monster> zombie_tactics$mine_goal;
    @Unique private int zombieTactics$climbedCount = 0;
    @Unique private boolean zombieTactics$isClimbing = false;
    @Final @Shadow private static Predicate<Difficulty> DOOR_BREAKING_PREDICATE;

    @Shadow public abstract boolean canBreakDoors(); // This just makes path finding

    /**
     * I do not want to see that zombies burn
     * @author PICOPress
     * @reason overwrite this function
     */
    @Overwrite
    public boolean isSunSensitive() {
        return Config.sunSensitive;
    }

    // Modifying Attack range
    @Override
    protected @NotNull AABB getAttackBoundingBox() {
        Entity entity = this.getVehicle();
        AABB aabb;
        if (entity != null) {
            AABB aabb1 = entity.getBoundingBox();
            AABB aabb2 = this.getBoundingBox();
            aabb = new AABB(Math.min(aabb2.minX, aabb1.minX),
                    Math.min(aabb2.minY, aabb1.minY),
                    Math.min(aabb2.minZ, aabb1.minZ),
                    Math.max(aabb2.maxX, aabb1.maxX),
                    Math.max(aabb2.maxY, aabb1.maxY),
                    Math.max(aabb2.maxZ, aabb1.maxZ));
        } else {
            aabb = this.getBoundingBox();
        }
        return aabb.inflate(Config.attackRange, Config.attackRange, Config.attackRange);
    }

    protected ZombieMixin(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isPersistenceRequired() {
        // randomly set PersistenceRequired
        // how... how can I use random... why does it crash...?
        //if(new Random().nextDouble(0, 1) < Config.persistenceChance)
        return true;
    }

    // fixes that doing both mining and attacking
    @Inject(method = "doHurtTarget", at = @At("HEAD"))
    public void doHurtTargetHead(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if(zombie_tactics$mine_goal != null) zombie_tactics$mine_goal.mine.doMining = false;
    }

    // Healing zombie
    @Inject(method = "doHurtTarget", at = @At("TAIL"))
    public void doHurtTargetTail(Entity ent, CallbackInfoReturnable<Boolean> ci) {
        if(ent instanceof LivingEntity) {
            if(this.getHealth() <= this.getMaxHealth())
                this.heal((float)Config.healAmount);
        }
        // reset invulnerable time
        if(Config.noMercy) ent.invulnerableTime = 0;
    }

    // For climbing
    @Override
    public void push(@NotNull Entity entity) {
        if(Config.zombiesClimbing && entity instanceof Zombie &&
                horizontalCollision) {
            if(zombieTactics$climbedCount < 120) {
                final Vec3 v = getDeltaMovement();
                setDeltaMovement(v.x, Config.climbingSpeed, v.z);
                zombieTactics$isClimbing = true;
                ++ zombieTactics$climbedCount;
            }
        }
        super.push(entity);
    }

    // zombie doesn't take fall damage when climbing
    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
        if(zombieTactics$isClimbing && onGround) {
            fallDistance = 0;
            zombieTactics$isClimbing = false;
            zombieTactics$climbedCount = 0;
        }
        super.checkFallDamage(y, onGround, state, pos);
    }

    // reset crack progress if a zombie died when mining
    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        if(zombie_tactics$mine_goal != null && zombie_tactics$mine_goal.mine.doMining)
            this.level().destroyBlockProgress(this.getId(), zombie_tactics$mine_goal.mine.bp, -1);
    }

    /**
     * ZombieMineGoal doesn't use zombie-exclusive things
     * @author PICOPress
     * @reason to overwrite
     */
    @Overwrite
    public void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new ZombieAttackGoal((Zombie)(Object)this,
                Config.aggressiveSpeed, true));
        if (Config.targetAnimals) {
            this.targetSelector.addGoal(Config.targetAnimalsPriority,
                    new NearestAttackableTargetGoal<>(this, Animal.class, false));
        }

        zombie_tactics$mine_goal = new ZombieMineGoal<>(this);
        if (Config.mineBlocks)
            this.goalSelector.addGoal(Config.miningPriority, zombie_tactics$mine_goal);

        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this,
                1.0, false, 4, this::canBreakDoors));

        this.goalSelector.addGoal(7,
                new WaterAvoidingRandomStrollGoal(this, 1.0));

        this.targetSelector.addGoal(1,
                (new HurtByTargetGoal(this)).setAlertOthers(ZombifiedPiglin.class));

        if(Config.attackInvisible) {
            this.targetSelector.addGoal(2,
                    new NearestTargetGoal<>(this, Player.class, false));

            this.targetSelector.addGoal(3,
                    new NearestTargetGoal<>(this, AbstractVillager.class, false));

            this.targetSelector.addGoal(3,
                    new NearestTargetGoal<>(this, IronGolem.class, true));
        } else {
            this.targetSelector.addGoal(2,
                    new NearestAttackableTargetGoal<>(this, Player.class, false));

            this.targetSelector.addGoal(3,
                    new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));

            this.targetSelector.addGoal(3,
                    new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        }

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(
                this, Turtle.class, 10, true, false, Turtle.BABY_ON_LAND_SELECTOR));

        this.goalSelector.addGoal(1, new BreakDoorGoal(this, DOOR_BREAKING_PREDICATE));
    }
}

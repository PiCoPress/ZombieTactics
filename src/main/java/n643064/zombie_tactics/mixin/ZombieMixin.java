package n643064.zombie_tactics.mixin;

import n643064.zombie_tactics.*;
import n643064.zombie_tactics.attachments.MiningData;
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

import net.tslat.smartbrainlib.api.SmartBrainOwner;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

@Mixin(Zombie.class)
public abstract class ZombieMixin extends Monster implements SmartBrainOwner<ZombieMixin> {
    @Unique private boolean zombieTactics$isClimbing = false;
    @Unique private int zombieTactics$climbedCount = 0;
    @Final @Shadow private static Predicate<Difficulty> DOOR_BREAKING_PREDICATE;

    @Shadow public abstract boolean canBreakDoors(); // This just makes path finding

    /**
     * I do not want to see that zombies burn
     * @author PICOPress
     * @reason overwrite this function
     */
    @Overwrite
    protected boolean isSunSensitive() {
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
            aabb = new AABB(Math.min(aabb2.minX, aabb1.minX), Math.min(aabb2.minY, aabb1.minY), Math.min(aabb2.minZ, aabb1.minZ),
                    Math.max(aabb2.maxX, aabb1.maxX), Math.max(aabb2.maxY, aabb1.maxY), Math.max(aabb2.maxZ, aabb1.maxZ));
        } else {
            aabb = this.getBoundingBox();
        }
        // I think it is a range
        return aabb.inflate(Config.attackRange, Config.attackRange, Config.attackRange);
    }

    protected ZombieMixin(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    // fixes that doing both mining and attacking
    @Inject(method = "doHurtTarget", at = @At("HEAD"))
    public void doHurtTargetHead(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        this.getData(Main.ZOMBIE_MINING).doMining = false;
        // Wither is zombie's friend
        // System.out.println(entity.getType().is(EntityTypeTags.WITHER_FRIENDS));
    }

    // Healing zombie
    @Inject(method = "doHurtTarget", at = @At("TAIL"))
    public void doHurtTargetTail(Entity ent, CallbackInfoReturnable<Boolean> ci) {
        if(ent instanceof LivingEntity) {
            if(this.getHealth()<= this.getMaxHealth())
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
        MiningData md = this.getData(Main.ZOMBIE_MINING);
        if(md.doMining)
            this.level().destroyBlockProgress(this.getId(), md.bp, -1);
    }

    /**
     * Force Object casting is required to load Mixin correctly, but linter warns those.
     * By using SuppressWarnings, highlights can be disabled.
     *
     */
    @SuppressWarnings("all")
    @Overwrite
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new ZombieAttackGoal((Zombie)(Object)this,
                Config.aggressiveSpeed, true));
        if (Config.targetAnimals) {
            this.targetSelector.addGoal(Config.targetAnimalsPriority,
                    new NearestAttackableTargetGoal<>(this, Animal.class,
                            Config.targetAnimalsVisibility));
        }
        if (Config.mineBlocks)
            this.goalSelector.addGoal(Config.miningPriority,
                    new ZombieMineGoal<>((Zombie)(Object)this));

        this.goalSelector.addGoal(6, new MoveThroughVillageGoal(this,
                1.0, false, 4, this::canBreakDoors));

        this.goalSelector.addGoal(7,
                new WaterAvoidingRandomStrollGoal(this, 1.0));

        this.targetSelector.addGoal(1,
                (new HurtByTargetGoal(this)).setAlertOthers(ZombifiedPiglin.class));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this,
                Player.class, false));

        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                AbstractVillager.class, false));

        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                IronGolem.class, true));

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this,
                Turtle.class, 10, true, false,
                Turtle.BABY_ON_LAND_SELECTOR));

        this.goalSelector.addGoal(1, new BreakDoorGoal(this, DOOR_BREAKING_PREDICATE));
    }
}

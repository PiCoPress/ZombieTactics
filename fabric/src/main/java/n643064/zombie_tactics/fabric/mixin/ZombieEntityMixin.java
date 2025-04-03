package n643064.zombie_tactics.fabric.mixin;

import n643064.zombie_tactics.fabric.Config;
import n643064.zombie_tactics.fabric.Main;
import n643064.zombie_tactics.fabric.attachments.MiningData;
import n643064.zombie_tactics.fabric.mining.ZombieMineGoal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

@Mixin(ZombieEntity.class)
// SBL isn't work
public abstract class ZombieEntityMixin extends HostileEntity /*implements SmartBrainOwner<ZombieEntityMixin>*/ {
    @Unique private boolean zombieTactics$isClimbing = false;
    @Unique private int zombieTactics$climbedCount = 0;
    @Final @Shadow private static Predicate<Difficulty> DOOR_BREAK_DIFFICULTY_CHECKER;

    @Shadow public abstract boolean canBreakDoors(); // This just makes path finding

    /**
     * I do not want to see that zombies burn
     * @author PICOPress
     * @reason overwrite this function
     */
    @Overwrite
    public boolean burnsInDaylight() {
        return Config.sunSensitive;
    }

    // Modifying Attack range
    @Override
    protected @NotNull Box getAttackBox() {
        Entity entity = this.getVehicle();
        Box aabb;
        if (entity != null) {
            Box aabb1 = entity.getBoundingBox();
            Box aabb2 = this.getBoundingBox();
            aabb = new Box(Math.min(aabb2.minX, aabb1.minX),
                    Math.min(aabb2.minY, aabb1.minY),
                    Math.min(aabb2.minZ, aabb1.minZ),
                    Math.max(aabb2.maxX, aabb1.maxX),
                    Math.max(aabb2.maxY, aabb1.maxY),
                    Math.max(aabb2.maxZ, aabb1.maxZ));
        } else {
            aabb = this.getBoundingBox();
        }
        return aabb.expand(Config.attackRange, Config.attackRange, Config.attackRange);
    }

    public ZombieEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    // fixes that doing both mining and attacking
    @Inject(method = "tryAttack", at = @At("HEAD"))
    public void tryAttackHead(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        this.getAttachedOrSet(Main.ZOMBIE_MINING, new MiningData()).doMining = false;
    }

    // Healing zombie
    @Inject(method = "tryAttack", at = @At("TAIL"))
    public void tryAttackTail(Entity ent, CallbackInfoReturnable<Boolean> ci) {
        if(ent instanceof LivingEntity) {
            if(this.getHealth() <= this.getMaxHealth())
                this.heal((float)Config.healAmount);
        }
        // reset invulnerable time
        if(Config.noMercy) ent.timeUntilRegen = 0;
    }

    // For climbing
    @Override
    public void pushAwayFrom(@NotNull Entity entity) {
        if(Config.zombiesClimbing && entity instanceof ZombieEntity &&
                horizontalCollision) {
            if(zombieTactics$climbedCount < 120) {
                final Vec3d v = getVelocity();
                setVelocity(v.x, Config.climbingSpeed, v.z);
                zombieTactics$isClimbing = true;
                ++ zombieTactics$climbedCount;
            }
        }
        super.pushAwayFrom(entity);
    }

    // zombie doesn't take fall damage when climbing
    @Override
    protected void fall(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
        if(zombieTactics$isClimbing && onGround) {
            fallDistance = 0;
            zombieTactics$isClimbing = false;
            zombieTactics$climbedCount = 0;
        }
        super.fall(y, onGround, state, pos);
    }

    // reset crack progress if a zombie died when mining
    @Override
    public void remove(@NotNull RemovalReason source) {
        super.remove(source);
        MiningData md = this.getAttachedOrSet(Main.ZOMBIE_MINING, new MiningData());
        if(md.doMining)
            this.getWorld().setBlockBreakingInfo(this.getId(), md.bp, -1);
    }

    /**
     * Force Object casting is required to load Mixin correctly, but linter warns those.
     * By using SuppressWarnings, highlights can be disabled.
     * @reason no..?
     * @author PICOPRESS
     */
    @Overwrite
    @SuppressWarnings("all")
    public void initGoals() {
        this.goalSelector.add(1, new ZombieAttackGoal((ZombieEntity)(Object)this,
                Config.aggressiveSpeed, false));

        if(Config.targetAnimals)
            this.targetSelector.add(Config.targetAnimalsPriority,
                    new ActiveTargetGoal<>(this, AnimalEntity.class,
                            Config.targetAnimalsVisibility));

        if(Config.mineBlocks)
            this.goalSelector.add(Config.miningPriority,
                    new ZombieMineGoal<>((ZombieEntity)(Object)this));

        this.goalSelector.add(6, new MoveThroughVillageGoal(this,
                1.0, false, 4, this::canBreakDoors));

        this.goalSelector.add(7,
                new WanderAroundFarGoal(this, 1.0));

        this.targetSelector.add(1,
                (new RevengeGoal(this)).setGroupRevenge(ZombifiedPiglinEntity.class));

        this.targetSelector.add(2, new ActiveTargetGoal<>(this,
                PlayerEntity.class, false));

        this.targetSelector.add(3, new ActiveTargetGoal<>(this,
                MerchantEntity.class, false));

        this.targetSelector.add(3, new ActiveTargetGoal<>(this,
                IronGolemEntity.class, true));

        this.targetSelector.add(5, new ActiveTargetGoal<>(this,
                TurtleEntity.class, 10, true, false,
                TurtleEntity.BABY_TURTLE_ON_LAND_FILTER));

        this.goalSelector.add(1, new BreakDoorGoal(this, DOOR_BREAK_DIFFICULTY_CHECKER));
    }
}

package io.github.imurx.tameablefoxes.mixin;


import io.github.imurx.tameablefoxes.TameableEntity;
import io.github.imurx.tameablefoxes.ai.SitGoal;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.ServerConfigHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(FoxEntity.class)
public abstract class TameableFox extends AnimalEntity implements TameableEntity {

    @Shadow private Goal followChickenAndRabbitGoal;
    @Shadow private Goal followBabyTurtleGoal;
    @Shadow private Goal followFishGoal;
    @Shadow public abstract void setSitting(boolean sitting);
    @Shadow public abstract boolean isBreedingItem(ItemStack stack);
    @Shadow public abstract void setTarget(@Nullable LivingEntity target);
    @Shadow abstract void stopActions();

    @Unique
    private static final TrackedData<Optional<UUID>> REAL_OWNER = DataTracker.registerData(FoxEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    @Unique
    private static final TrackedData<Byte> TAMEABLE_FLAGS = DataTracker.registerData(FoxEntity.class, TrackedDataHandlerRegistry.BYTE);
    @Unique
    private static final int TAMED_FLAG = 1;
    @Unique
    private static final int SITTINGOWNER_FLAG = 2;

    protected TameableFox(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    protected void onInitGoals(CallbackInfo ci)  {
        this.goalSelector.add(2, new SitGoal((FoxEntity) (Object) this));
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    protected void onInitDataTracker(CallbackInfo info) {
        this.dataTracker.startTracking(TAMEABLE_FLAGS, (byte) 0);
        this.dataTracker.startTracking(REAL_OWNER, Optional.empty());
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void onWriteToNbt(NbtCompound nbt, CallbackInfo ci) {
        if(this.getOwnerUuid() != null) {
            nbt.putUuid("Owner", this.getOwnerUuid());
        }
        nbt.putBoolean("SittingByOwner", this.tameablefoxes$isSittingByOwner());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void onReadFromNbt(NbtCompound nbt, CallbackInfo ci) {
        UUID uuid;
        if (nbt.containsUuid("Owner")) {
            uuid = nbt.getUuid("Owner");
        } else {
            String string = nbt.getString("Owner");
            uuid = ServerConfigHandler.getPlayerUuidByName(this.getServer(), string);
        }

        if (uuid != null) {
            try {
                this.tameablefoxes$setOwnerUuid(uuid);
                this.tameablefoxes$setTamed(true);
            } catch (Throwable var4) {
                this.tameablefoxes$setTamed(false);
            }
        }
        this.tameablefoxes$setSittingByOwner(nbt.getBoolean("SittingByOwner"));
    }

    @Inject(method = "canTrust", at = @At("HEAD"), cancellable = true)
    void onCanTrust(UUID uuid, CallbackInfoReturnable<Boolean> cir) {
        if(this.getOwnerUuid() == uuid) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setSitting", at = @At("HEAD"), cancellable = true)
    void onSetSitting(boolean sitting, CallbackInfo ci) {
        if(this.tameablefoxes$isSittingByOwner() && !sitting) {
            this.tameablefoxes$setSittingByOwner(false);
            ci.cancel();
        }
    }

    @Unique
    private boolean getTamedFlag(int mask) {
        return (this.dataTracker.get(TAMEABLE_FLAGS) & mask) != 0;
    }

    @Unique
    private void setTamedFlag(int mask, boolean flag) {
        byte flags = this.dataTracker.get(TAMEABLE_FLAGS);
        if(flag) {
            this.dataTracker.set(TAMEABLE_FLAGS, (byte) (flags | mask));
        } else {
            this.dataTracker.set(TAMEABLE_FLAGS, (byte) (flags & ~mask));
        }
    }

    public boolean tameablefoxes$isTamed() {
        return this.getTamedFlag(TAMED_FLAG);
    }

    public void tameablefoxes$setTamed(boolean tamed) {
        this.setTamedFlag(TAMED_FLAG, tamed);
        this.onTamedChanged();
    }

    @Unique
    protected void onTamedChanged() {
    }


    @Nullable
    @Override
    public UUID getOwnerUuid() {
        return this.dataTracker.get(REAL_OWNER).orElse(null);
    }

    public void tameablefoxes$setOwnerUuid(@Nullable UUID uuid) {
        this.dataTracker.set(REAL_OWNER, Optional.ofNullable(uuid));
    }

    public void tameablefoxes$setOwner(PlayerEntity player) {
        this.tameablefoxes$setTamed(true);
        this.tameablefoxes$setOwnerUuid(player.getUuid());
        if (player instanceof ServerPlayerEntity) {
            Criteria.TAME_ANIMAL.trigger((ServerPlayerEntity)player, this);
        }
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        try {
            UUID uUID = this.getOwnerUuid();
            return uUID == null ? null : this.world.getPlayerByUuid(uUID);
        } catch (IllegalArgumentException var2) {
            return null;
        }
    }

    public boolean tameablefoxes$isOwner(LivingEntity entity) {
        return entity == this.getOwner();
    }

    public boolean tameablefoxes$isSittingByOwner() {
        return this.getTamedFlag(SITTINGOWNER_FLAG);
    }

    public void tameablefoxes$setSittingByOwner(boolean sitting) {
        this.setTamedFlag(SITTINGOWNER_FLAG, sitting);
        this.setSitting(sitting);
    }

    public AbstractTeam getScoreboardTeam() {
        if (this.tameablefoxes$isTamed()) {
            LivingEntity livingEntity = this.getOwner();
            if (livingEntity != null) {
                return livingEntity.getScoreboardTeam();
            }
        }

        return super.getScoreboardTeam();
    }

    public boolean isTeammate(Entity other) {
        if (this.tameablefoxes$isTamed()) {
            LivingEntity livingEntity = this.getOwner();
            if (other == livingEntity) {
                return true;
            }

            if (livingEntity != null) {
                return livingEntity.isTeammate(other);
            }
        }

        return super.isTeammate(other);
    }

    public void onDeath(DamageSource source) {
        if (!this.world.isClient && this.world.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES) && this.getOwner() instanceof ServerPlayerEntity) {
            this.getOwner().sendSystemMessage(this.getDamageTracker().getDeathMessage(), Util.NIL_UUID);
        }

        super.onDeath(source);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        Item item = itemStack.getItem();
        if(this.world.isClient) {
            if(this.isBreedingItem(itemStack)) return super.interactMob(player, hand);
            boolean consume = this.tameablefoxes$isOwner(player) || this.tameablefoxes$isTamed();
            return consume ? ActionResult.CONSUME : ActionResult.PASS;
        } else {
            if(this.tameablefoxes$isTamed()) {
                ActionResult actionResult = super.interactMob(player, hand);
                if ((!actionResult.isAccepted()) && this.tameablefoxes$isOwner(player)) {
                    boolean sitting = this.tameablefoxes$isSittingByOwner();
                    this.navigation.stop();
                    this.setMovementSpeed(0);
                    this.stopActions();
                    this.setTarget(null);
                    this.tameablefoxes$setSittingByOwner(!sitting);

                    return ActionResult.SUCCESS;
                }

                return actionResult;
            }
            return super.interactMob(player, hand);
        }
    }
}

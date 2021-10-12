package io.github.imurx.tameablefoxes.mixin;


import io.github.imurx.tameablefoxes.TameableEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.ServerConfigHandler;
import net.minecraft.server.network.ServerPlayerEntity;
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

    @Shadow
    @Final
    private static TrackedData<Optional<UUID>> OWNER;
    @Unique
    private static final TrackedData<Optional<UUID>> REAL_OWNER = DataTracker.registerData(FoxEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    @Unique
    private static final TrackedData<Boolean> TAMED = DataTracker.registerData(FoxEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    protected TameableFox(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    protected void onInitDataTracker(CallbackInfo info) {
        this.dataTracker.startTracking(TAMED, false);
        this.dataTracker.startTracking(REAL_OWNER, Optional.empty());
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void onWriteToNbt(NbtCompound nbt, CallbackInfo ci) {
        if(this.getOwnerUuid() != null) {
            nbt.putUuid("Owner", this.getOwnerUuid());
        }
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
    }

    @Inject(method = "canTrust", at = @At("HEAD"), cancellable = true)
    void onCanTrust(UUID uuid, CallbackInfoReturnable cir) {
        if(this.getOwnerUuid() == uuid) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    public boolean tameablefoxes$isTamed() {
        return this.dataTracker.get(TAMED);
    }

    @Unique
    public void tameablefoxes$setTamed(boolean tamed) {
        this.dataTracker.set(TAMED, tamed);
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

    @Unique
    public void tameablefoxes$setOwnerUuid(@Nullable UUID uuid) {
        this.dataTracker.set(REAL_OWNER, Optional.ofNullable(uuid));
    }

    @Unique
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

    @Unique
    public boolean tameablefoxes$isOwner(LivingEntity entity) {
        return entity == this.getOwner();
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
}

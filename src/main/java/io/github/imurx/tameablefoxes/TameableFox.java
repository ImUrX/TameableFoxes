package io.github.imurx.tameablefoxes;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface TameableFox extends Tameable {
    boolean tameablefoxes$isTamed();
    void tameablefoxes$setTamed(boolean tamed);
    void tameablefoxes$setOwnerUuid(@Nullable UUID uuid);
    void tameablefoxes$setOwner(PlayerEntity player);
    boolean tameablefoxes$isOwner(LivingEntity entity);
    boolean tameablefoxes$isSittingByOwner();
    void tameablefoxes$setSittingByOwner(boolean sitting);

    @Nullable
    @Override
    LivingEntity getOwner();
}

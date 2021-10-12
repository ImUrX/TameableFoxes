package io.github.imurx.tameablefoxes;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface TameableEntity extends Tameable {
    public boolean tameablefoxes$isTamed();
    public void tameablefoxes$setTamed(boolean tamed);
    public void tameablefoxes$setOwnerUuid(@Nullable UUID uuid);
    public void tameablefoxes$setOwner(PlayerEntity player);
    public boolean tameablefoxes$isOwner(LivingEntity entity);
}

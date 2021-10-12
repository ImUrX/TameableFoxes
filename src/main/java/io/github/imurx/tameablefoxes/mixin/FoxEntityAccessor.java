package io.github.imurx.tameablefoxes.mixin;

import net.minecraft.entity.passive.FoxEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;

@Mixin(FoxEntity.class)
public interface FoxEntityAccessor {
    @Invoker
    boolean invokeCanTrust(UUID uuid);
}

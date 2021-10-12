package io.github.imurx.tameablefoxes.mixin;

import io.github.imurx.tameablefoxes.TameableEntity;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net/minecraft/entity/passive/FoxEntity$MateGoal")
public abstract class TameableFoxMateGoal extends AnimalMateGoal {
    public TameableFoxMateGoal(AnimalEntity animal, double chance) {
        super(animal, chance);
    }

    @Inject(
        method = "breed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/AnimalEntity;setBreedingAge(I)V",
            ordinal = 0
        ),
        locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    protected void onBreed(CallbackInfo ci, ServerWorld serverWorld, FoxEntity baby) {
        ServerPlayerEntity serverPlayerEntity = this.animal.getLovingPlayer();
        ServerPlayerEntity serverPlayerEntity2 = this.mate.getLovingPlayer();
        ServerPlayerEntity mainPlayer = serverPlayerEntity;
        if (serverPlayerEntity == null) {
            mainPlayer = serverPlayerEntity2;
        }
        TameableEntity realBaby = (TameableEntity) baby;
        if(((FoxEntityAccessor) this.animal).invokeCanTrust(mainPlayer.getUuid()) && ((FoxEntityAccessor) this.mate).invokeCanTrust(mainPlayer.getUuid())) {
            realBaby.tameablefoxes$setOwner(mainPlayer);
        }
    }
}

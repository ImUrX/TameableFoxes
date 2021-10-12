package io.github.imurx.tameablefoxes.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import io.github.imurx.tameablefoxes.TameableEntity;
import net.minecraft.entity.passive.FoxEntity;

import java.util.EnumSet;

public class SitGoal extends Goal {
    private final FoxEntity tameable;

    public SitGoal(FoxEntity tameable) {
        this.tameable = tameable;
        this.setControls(EnumSet.of(Control.JUMP, Control.MOVE));
    }

    public boolean shouldContinue() {
        return ((TameableEntity) this.tameable).tameablefoxes$isSittingByOwner();
    }

    public boolean canStart() {
        if (!((TameableEntity) this.tameable).tameablefoxes$isTamed()) {
            return false;
        } else if (this.tameable.isInsideWaterOrBubbleColumn()) {
            return false;
        } else if (!this.tameable.isOnGround()) {
            return false;
        } else {
            LivingEntity livingEntity = ((TameableEntity) this.tameable).getOwner();
            if (livingEntity == null) {
                return true;
            } else {
                return (!(this.tameable.squaredDistanceTo(livingEntity) < 144.0D) || livingEntity.getAttacker() == null) && this.shouldContinue();
            }
        }
    }

    public void start() {
        this.tameable.getNavigation().stop();
        ((TameableEntity) this.tameable).tameablefoxes$setSittingByOwner(true);
    }

    public void stop() {
        ((TameableEntity) this.tameable).tameablefoxes$setSittingByOwner(false);
    }
}
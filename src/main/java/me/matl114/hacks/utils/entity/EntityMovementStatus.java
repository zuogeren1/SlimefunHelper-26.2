package me.matl114.hacks.utils.entity;

import me.matl114.hacks.modules.move.PlayerStateManager;
import me.matl114.hacks.utils.EntityUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class EntityMovementStatus<T extends Entity> {
    public EntityMovementStatus(T entity) {
        this.entity = entity;
        onGround = entity.onGround();
        horizontalCollision = entity.horizontalCollision;
        verticalCollision = entity.verticalCollision;
        groundCollision = entity.verticalCollisionBelow;
        pos = entity.position();
        pitch = entity.getXRot();
        yaw = entity.getYRot();
        vec = entity.getDeltaMovement();
        speed = entity.flyDist;
        distanceTraveled = entity.moveDist;
        sprinting = entity.isSprinting();
        touchingWater = entity.isInWater();
        collidedSoftly = entity.minorHorizontalCollision;
        submergedInWater = entity.isUnderWater();
        inPowderSnow = entity.isInPowderSnow;
    }

    public T entity;
    public boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean groundCollision;
    public boolean collidedSoftly;
    public Vec3 pos;
    public float pitch;
    public float yaw;
    public Vec3 vec;
    public float speed;
    public float distanceTraveled;
    public boolean sprinting;
    public boolean touchingWater;
    public boolean submergedInWater;
    public boolean inPowderSnow;

    public void restore() {
        this.entity.horizontalCollision = horizontalCollision;
        this.entity.verticalCollision = verticalCollision;
        this.entity.verticalCollisionBelow = groundCollision;
        this.entity.minorHorizontalCollision = collidedSoftly;
        this.restorePosRot();
        this.restoreOnGround();

        this.entity.setDeltaMovement(vec);
        this.entity.flyDist = speed;
        this.entity.moveDist = distanceTraveled;
        this.entity.setSprinting(sprinting);
    }

    public void restoreOnGround() {
        this.entity.setOnGround(onGround);
    }

    public void restorePosRot() {
        this.restoreRotation();
        this.restorePos();
    }

    public void restoreRotation() {
        EntityUtils.setEntityPitchSafe(this.entity, pitch);
        if (this.entity instanceof LocalPlayer clientPlayer) {
            PlayerStateManager.setPlayerYawSafe(clientPlayer, yaw);
        } else {
            EntityUtils.setEntityYawSafe(this.entity, yaw);
        }
    }

    public void restorePos() {
        this.entity.setPos(pos);
        entity.wasTouchingWater = touchingWater;
        entity.wasEyeInWater = submergedInWater;
        entity.isInPowderSnow = inPowderSnow;
    }

    public Vec3 calculateLastMoveVelocity(int forward, int sideward) {
        if (this.entity instanceof LivingEntity livingEntity) {
            Vec2 vec2f = new Vec2(sideward, forward).normalized();
            vec2f = EntityUtils.applyMovementFactors(this.entity, vec2f);
            Vec3 vec3d2 = new Vec3(vec2f.x, this.entity instanceof LivingEntity lv ? lv.yya : 0.0F, vec2f.y);
            float f = this.entity.onGround()
                    ? this.entity
                            .level()
                            .getBlockState(this.entity.getBlockPosBelowThatAffectsMyMovement())
                            .getBlock()
                            .getFriction()
                    : 1.0F;
            float speed = livingEntity.getFrictionInfluencedSpeed(f);
            Vec3 more = EntityUtils.movementInputToVelocity(vec3d2, speed, yaw);
            Vec3 velocity = this.vec.add(more);
            velocity = livingEntity.handleOnClimbable(velocity);
            return velocity;
        } else {
            return this.entity.getDeltaMovement();
        }
    }
}

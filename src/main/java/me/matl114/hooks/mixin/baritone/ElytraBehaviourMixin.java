package me.matl114.hooks.mixin.baritone;

import baritone.api.utils.IPlayerController;
import baritone.behavior.InventoryBehavior;
import baritone.process.elytra.ElytraBehavior;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Predicate;
import me.matl114.hacks.modules.move.ElytraExtra;
import me.matl114.hacks.modules.move.FloatingUtils;
import me.matl114.hacks.modules.survival.BaritoneFix;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Environment(EnvType.CLIENT)
@Mixin(ElytraBehavior.class)
public abstract class ElytraBehaviourMixin {
    @WrapOperation(
            method = {
                "a(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;ZZ)V",
                "tickUseFireworks(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;ZZ)V"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lbaritone/api/utils/IPlayerController;processRightClick(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"),
            require = 0)
    public InteractionResult onUseFireworks(
            IPlayerController instance,
            LocalPlayer player,
            Level world,
            InteractionHand hand,
            Operation<InteractionResult> original) {
        if (BaritoneFix.INSTANCE.enableGhostHandFireworks.get()) {
            ElytraExtra.INSTANCE.sendCustomUseFireworkPacket();
            return InteractionResult.SUCCESS;
        }
        return original.call(instance, player, world, hand);
    }

    @WrapOperation(
            method = "a(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;ZZ)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lbaritone/behavior/InventoryBehavior;a(ZLjava/util/function/Predicate;)Z"),
            require = 0,
            remap = false)
    private boolean onCancelInventorySwap(
            InventoryBehavior instance,
            boolean b,
            Predicate<? super ItemStack> predicate,
            Operation<Boolean> original) {
        if (BaritoneFix.INSTANCE.enableGhostHandFireworks.get()) {
            return true;
        }
        return original.call(instance, b, predicate);
    }

    @WrapOperation(
            method = "tickUseFireworks(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;ZZ)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lbaritone/behavior/InventoryBehavior;throwaway(ZLjava/util/function/Predicate;)Z"),
            require = 0,
            remap = false)
    private boolean onCancelInventorySwap2(
            InventoryBehavior instance,
            boolean b,
            Predicate<? super ItemStack> predicate,
            Operation<Boolean> original) {
        if (BaritoneFix.INSTANCE.enableGhostHandFireworks.get()) {
            return true;
        }
        return original.call(instance, b, predicate);
    }

    @WrapOperation(
            method = {
                "a(Lbaritone/process/elytra/ElytraBehavior$SolverContext;Lnet/minecraft/world/phys/Vec3;ILit/unimi/dsi/fastutil/floats/FloatArrayList;III)Lbaritone/process/elytra/ElytraBehavior$PitchResult;",
                "Lbaritone/process/elytra/ElytraBehavior;simulate(Lbaritone/process/elytra/ElytraBehavior$SolverContext;Lnet/minecraft/world/phys/Vec3;FIII)Ljava/util/List;"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"),
            require = 0)
    private AABB fixHitBoxCalculation(AABB instance, double x, double y, double z, Operation<AABB> original) {
        if (BaritoneFix.INSTANCE.fixSimulateError.get()) {
            return instance.move(x, y, z);
        }
        return original.call(instance, x, y, z);
    }
    //    @Unique
    //    @Final
    //    private static final VarHandle boxHandle = initialize();
    //    @Unique
    //    private static VarHandle initialize(){
    //        try{
    //            Class<?> clazz = Class.forName(
    //                "baritone.process.elytra.ElytraBehavior$SolverContext");
    //            Field field = Arrays.stream(clazz.getFields()).filter(s -> s.getType() == Box.class).filter(s ->
    // !Modifier.isStatic(s.getModifiers())).findAny().orElseThrow();
    //            field.setAccessible(true);
    //            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
    //            return lookup.unreflectVarHandle(field);
    //        }catch (Throwable e){
    //            throw new RuntimeException(e);
    //        }
    ////    }
    //    @Unique
    //    Box cachedBox;

    //    @Inject(method = {"a(Lbaritone/api/event/events/TickEvent;)V",
    // "Lbaritone/process/elytra/ElytraBehavior;onPostTick(Lbaritone/api/event/events/TickEvent;)V"}, at = @At(value =
    // "INVOKE", target =
    // "Lbaritone/process/elytra/ElytraBehavior$SolverContext;<init>(Lbaritone/process/elytra/ElytraBehavior;Z)V", shift
    // = At.Shift.BEFORE), require = 0)
    //    private void onInitialize(TickEvent par1, CallbackInfo ci){
    //        if(BaritoneFix.INSTANCE.fixErrorFly.get()){
    //            Box box1 = BaritoneFix.INSTANCE.processBoxOfElytraFlight();
    //            if(box1 != null){
    //                var pl =  Minecraft.getInstance().player;
    //                cachedBox = pl.getBoundingBox();
    //                pl.setBoundingBox(box1);
    //            }
    //        }
    //    }
    //    @Inject(method = {"a(Lbaritone/api/event/events/TickEvent;)V",
    // "Lbaritone/process/elytra/ElytraBehavior;onPostTick(Lbaritone/api/event/events/TickEvent;)V"}, at = @At(value =
    // "INVOKE", target =
    // "Lbaritone/process/elytra/ElytraBehavior$SolverContext;<init>(Lbaritone/process/elytra/ElytraBehavior;Z)V", shift
    // = At.Shift.AFTER), require = 0)
    //    private void onInitialize2(TickEvent par1, CallbackInfo ci){
    //        if(cachedBox != null){
    //            Minecraft.getInstance().player.setBoundingBox(cachedBox);
    //            cachedBox = null;
    //        }
    //    }
    //
    //    @Inject(method = "Lbaritone/process/elytra/ElytraBehavior;tick()V", at = @At(value = "INVOKE", target =
    // "Lbaritone/process/elytra/ElytraBehavior$SolverContext;<init>(Lbaritone/process/elytra/ElytraBehavior;Z)V", shift
    // = At.Shift.BEFORE), require = 0)
    //    private void onInitialize3(CallbackInfo ci){
    //        if(BaritoneFix.INSTANCE.fixErrorFly.get()){
    //            Box box1 = BaritoneFix.INSTANCE.processBoxOfElytraFlight();
    //            if(box1 != null){
    //                var pl =  Minecraft.getInstance().player;
    //                cachedBox = pl.getBoundingBox();
    //                pl.setBoundingBox(box1);
    //            }
    //        }
    //    }
    //
    //    @Inject(method = "Lbaritone/process/elytra/ElytraBehavior;tick()V", at = @At(value = "INVOKE", target =
    // "Lbaritone/process/elytra/ElytraBehavior$SolverContext;<init>(Lbaritone/process/elytra/ElytraBehavior;Z)V", shift
    // = At.Shift.AFTER), require = 0)
    //    private void onInitialize4(CallbackInfo ci){
    //        if(cachedBox != null){
    //            Minecraft.getInstance().player.setBoundingBox(cachedBox);
    //            cachedBox = null;
    //        }
    //    }

    @Inject(
            method = "Lbaritone/process/elytra/ElytraBehavior;tick()V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lbaritone/process/elytra/ElytraBehavior;logVerbose(Ljava/lang/String;)V",
                            ordinal = 2),
            require = 0,
            remap = false)
    private void onNoSolution3(CallbackInfo ci) {
        if (BaritoneFix.INSTANCE.freezeWhenFailCalculate.get()) {
            BaritoneFix.INSTANCE.logI18N("message.module.baritone-fix.freeze-all");
            FloatingUtils.INSTANCE.setGrimFloatingTick(true);
        }
    }

    @Inject(
            method = "Lbaritone/process/elytra/ElytraBehavior;tick()V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lbaritone/process/elytra/ElytraBehavior;logVerbose(Ljava/lang/String;)V",
                            ordinal = 3),
            require = 0,
            remap = false)
    private void onNoSolution4(CallbackInfo ci) {
        if (BaritoneFix.INSTANCE.freezeWhenFailCalculate.get()) {
            BaritoneFix.INSTANCE.logI18N("message.module.baritone-fix.freeze-pitch");
            FloatingUtils.INSTANCE.setGrimFloatingTick(true);
        }
    }

    @ModifyExpressionValue(
            method = {"a()V", "Lbaritone/process/elytra/ElytraBehavior;pathTo()V"},
            at = @At(value = "FIELD", target = "Lbaritone/api/Settings$Setting;value:Ljava/lang/Object;"),
            require = 0,
            remap = false)
    private Object onAutoJumpFix(Object original) {
        if (original instanceof Boolean bl) {
            if (BaritoneFix.INSTANCE.autoJumpFix.get()) {
                return false;
            }
        }
        return original;
    }
}

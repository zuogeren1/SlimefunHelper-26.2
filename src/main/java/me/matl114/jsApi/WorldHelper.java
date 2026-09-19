package me.matl114.jsApi;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.JavaOps;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import me.matl114.utils.ApiMethod;
import me.matl114.utils.ItemStackUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@ApiMethod
public class WorldHelper {
    private static final Minecraft mc = Minecraft.getInstance();

    public static BlockState getBlockState(Level world, BlockPos pos) {
        return world.getBlockState(pos);
    }

    public static Object getBlockData(Level world, BlockPos pos) throws Throwable {
        return JsMacrosBridge.getInstance().newBlockData(world.getBlockState(pos), world.getBlockEntity(pos), pos);
    }

    public static boolean isWorldClient(Level world) {
        return world.isClientSide();
    }

    public static void setBlockState(Level world, BlockPos pos, BlockState state) {
        mc.execute(() -> world.setBlockAndUpdate(pos, state));
    }

    public static FluidState getFluidState(Level world, BlockPos pos) {
        return world.getFluidState(pos);
    }

    public static BlockEntity getBlockEntity(Level world, BlockPos pos) {
        return world.getBlockEntity(pos);
    }

    public static Block getBlockOfState(BlockState state) {
        return state.getBlock();
    }

    public static BlockState getDefaultState(Block block) {
        return block.defaultBlockState();
    }

    public static String getBlockIdOfState(BlockState state) {
        return RegistryHelper.getIdInRegistry(BuiltInRegistries.BLOCK, state.getBlock());
    }

    public static Map<String, Object> getStateMap(BlockState state) {
        return (Map<String, Object>) BlockState.CODEC
                .encodeStart(ItemStackUtils.registry().createSerializationContext(JavaOps.INSTANCE), state)
                .getOrThrow();
    }

    public static BlockState createStateByMap(Map<String, Object> obj) {
        return BlockState.CODEC
                .decode(ItemStackUtils.registry().createSerializationContext(JavaOps.INSTANCE), obj)
                .getOrThrow()
                .getFirst();
    }

    public static boolean isInWorldBorder(int x, int y, int z) {
        return isInWorldBorder(new BlockPos(x, y, z));
    }

    public static boolean isInWorldBorder(Object pos0) {
        BlockPos pos = DataHelper.createBlockPos(pos0);
        return mc.level.getWorldBorder().isWithinBounds(pos);
    }

    public static Entity getEntityById(int i) throws ExecutionException, InterruptedException {
        FutureTask<Entity> futureTask = new FutureTask<>(() -> mc.level.getEntity(i));
        mc.execute(futureTask);
        return futureTask.get();
    }

    public static Entity getEntityByUid(Object obj) throws ExecutionException, InterruptedException {
        UUID uuid = (obj instanceof UUID uid) ? uid : UUID.fromString(obj.toString());

        FutureTask<Entity> futureTask =
                new FutureTask<>(() -> mc.level.getEntities().get(uuid));
        mc.execute(futureTask);
        return futureTask.get();
    }

    public static Entity getEntityByIdUnsafe(int i) {
        return mc.level.getEntity(i);
    }

    public static Entity getEntityByUidUnsafe(Object obj) {
        UUID uuid = (obj instanceof UUID uid) ? uid : UUID.fromString(obj.toString());
        return mc.level.getEntities().get(uuid);
    }

    public static List<Entity> getEntitiesByDistance() throws Throwable {
        return sortEntitiesByDistance(getEntities());
    }

    public static <T> List<T> sortEntitiesByDistance(List<T> en) {
        return (List<T>) (en.stream()
                .sorted(Comparator.comparingDouble(
                        (d) -> JsHelper.unwrap(d, Entity.class).position().distanceToSqr(mc.player.position())))
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    public static List<Entity> getEntities() throws ExecutionException, InterruptedException {
        FutureTask<List<Entity>> futureTask = new FutureTask<>(WorldHelper::getEntitiesUnsafe);
        mc.execute(futureTask);
        return futureTask.get();
    }

    public static List<Entity> getEntitiesUnsafe() {
        return ImmutableList.copyOf(mc.level.entitiesForRendering());
    }

    public static List<Entity> getEntities(double distance) throws ExecutionException, InterruptedException {
        List<Entity> list = getEntities();
        double dsSquared = distance * distance;
        return list.stream()
                .filter(s -> s.distanceToSqr(mc.player) <= dsSquared)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static List<Entity> getEntitiesInBox(Object center, int xhalf, int yhalf, int zhalf)
            throws ExecutionException, InterruptedException {
        Vec3 centerPos = JsHelper.unwrap(center, Vec3.class);
        AABB box = new AABB(centerPos.subtract(xhalf, yhalf, zhalf), centerPos.add(xhalf, yhalf, zhalf));
        List<Entity> list = new ArrayList<>();
        Future<Void> futureTask = new FutureTask<>(() -> {
            mc.level.getEntities().get(box, list::add);
            return null;
        });
        futureTask.get();
        return list;
    }
}

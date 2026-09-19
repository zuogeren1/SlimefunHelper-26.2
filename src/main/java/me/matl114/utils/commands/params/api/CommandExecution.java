package me.matl114.utils.commands.params.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.matl114.utils.ChatUtils;
import me.matl114.utils.Debug;
import me.matl114.utils.commands.interruption.InvalidExecutorError;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3d;

public interface CommandExecution {
    @Nullable
    public Player getExecutor();

    boolean hasPermission(String permission);

    default boolean isPlayer() {
        return getExecutor() instanceof Player;
    }

    public static CommandExecution sender(@Nonnull Player sender) {
        return new Sender(sender);
    }

    public void sendMessage(@Nonnull String message);

    public void sendMessage(Component message);

    public Vector2f getExecuteRot();

    @Nonnull
    public Vector3d getExecutePos();

    default Vector3d getExecuteEyePos() {
        if (getExecutor() instanceof Player pl) {
            return getExecutePos().add(0, pl.getEyeHeight(pl.getPose()), 0);
        } else {
            return getExecutePos();
        }
    }

    @Nonnull
    public Level getExecuteWorld();

    @Nonnull
    default Player getExecutorPlayer() {
        if (isPlayer()) {
            return (Player) getExecutor();
        } else {
            throw new InvalidExecutorError(false);
        }
    }

    public CommandExecution EMPTY = new Sender(null);

    public record Sender(Player sender) implements CommandExecution {

        @org.jetbrains.annotations.Nullable
        @Override
        public Player getExecutor() {
            return sender;
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public void sendMessage(@NotNull String message) {
            if (sender != null) {
                Debug.sendPlayer(ChatUtils.stringToText(message));
            }
        }

        @Override
        public void sendMessage(Component message) {
            if (sender != null) {
                Debug.sendPlayer(message);
            }
        }

        @Override
        public Vector2f getExecuteRot() {
            if (sender instanceof Player p) {
                return new Vector2f(p.getXRot(), p.getYRot());
            } else {
                return new Vector2f(0, 0);
            }
        }

        @Override
        public Vector3d getExecutePos() {
            if (sender instanceof Player p) {
                return new Vector3d(p.getX(), p.getY(), p.getZ());
            } else {
                return new Vector3d(0, 0, 0);
            }
        }

        @Override
        public Level getExecuteWorld() {
            return sender instanceof Player player ? player.level() : Minecraft.getInstance().level;
        }
    }

    public record System(boolean sout) implements CommandExecution {

        @org.jetbrains.annotations.Nullable
        @Override
        public Player getExecutor() {
            return null;
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public void sendMessage(@NotNull String message) {
            if (sout) {
                Debug.info(message);
            }
        }

        @Override
        public void sendMessage(Component message) {
            if (sout) {
                Debug.info(message);
            }
        }

        @Override
        public Vector2f getExecuteRot() {
            return new Vector2f(0, 0);
        }

        @Override
        public Vector3d getExecutePos() {
            return new Vector3d(0, 0, 0);
        }

        @Override
        public Level getExecuteWorld() {
            return Minecraft.getInstance().level;
        }
    }
}

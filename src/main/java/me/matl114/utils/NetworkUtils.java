package me.matl114.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;

@ApiMethod
public class NetworkUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    public static ByteBuf createBytebuf() {
        return Unpooled.buffer();
    }

    public static int generateNextSequence() {
        var re = mc.level.getBlockStatePredictionHandler().startPredicting();
        int seq = re.currentSequenceNr;
        re.close();
        return seq;
    }

    public static void restoreSequence(int sequenceRestore) {
        var re = mc.level.getBlockStatePredictionHandler();
        if (re.currentSequenceNr == sequenceRestore) {
            --re.currentSequenceNr;
        }
    }
}

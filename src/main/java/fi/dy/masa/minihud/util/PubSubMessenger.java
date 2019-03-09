package fi.dy.masa.minihud.util;

import com.mumfrey.liteloader.core.PluginChannels;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PubSubMessenger {
    public static final String CHANNEL_NAME = "carpet:pubsub";
    // reserve id 0 for now
    public static final int PACKET_C2S_SUBSCRIBE = 1;
    public static final int PACKET_C2S_UNSUBSCRIBE = 2;

    public static final int PACKET_S2C_UPDATE = 1;

    public static final int TYPE_NBT = 0;
    public static final int TYPE_STRING = 1;
    public static final int TYPE_INT = 2;
    public static final int TYPE_FLOAT = 3;
    public static final int TYPE_LONG = 4;
    public static final int TYPE_DOUBLE = 5;

    public static Map<String, Object> decodeUpdate(PacketBuffer data) throws IOException {
        int count = data.readVarInt();
        Map<String, Object> nodes = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String node = data.readString(32767);
            nodes.put(node, decodeValue(data));
        }
        return nodes;
    }

    private static Object decodeValue(PacketBuffer data) throws IOException {
        int type = data.readVarInt();
        switch (type) {
            case TYPE_NBT: {
                NBTTagCompound compound = CompressedStreamTools.read(new ByteBufInputStream(data), NBTSizeTracker.INFINITE);
                if (compound.hasKey("")) {
                    return compound.getTag("");
                } else {
                    return compound;
                }
            }
            case TYPE_STRING: return data.readString(32767);
            case TYPE_INT: return data.readInt();
            case TYPE_FLOAT: return data.readFloat();
            case TYPE_LONG: return data.readLong();
            case TYPE_DOUBLE: return data.readDouble();
        }
        throw new IllegalArgumentException("Unknown element type");
    }

    public static void subscribe(String... nodes) {
        subscribe(Arrays.asList(nodes));
    }

    public static void subscribe(Collection<String> nodes) {
        updateSubscriptions(PACKET_C2S_SUBSCRIBE, nodes);
    }

    public static void unsubscribe(String... nodes) {
        unsubscribe(Arrays.asList(nodes));
    }

    public static void unsubscribe(Collection<String> nodes) {
        updateSubscriptions(PACKET_C2S_UNSUBSCRIBE, nodes);
    }

    private static void updateSubscriptions(int updateType, Collection<String> nodes) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeVarInt(updateType);
        buf.writeVarInt(nodes.size());
        for (String node : nodes) buf.writeString(node);
        PacketSplitter.send(CHANNEL_NAME, buf, PluginChannels.ChannelPolicy.DISPATCH_ALWAYS);
    }
}

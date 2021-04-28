package ml.northwestwind.survivalspectator.data;

import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PositionData extends PersistentState {
    public final HashMap<UUID, Pair<Vec3d, RegistryKey<World>>> positions = Maps.newHashMap();
    public static final String NAME = "survivalspectator";

    public PositionData() {
        super(NAME);
    }

    public static PositionData get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(PositionData::new, NAME);
    }

    @Override
    public void fromTag(CompoundTag tag) {
        ListTag list = (ListTag) tag.get("spectators");
        if (list != null) {
            int i = 0;
            while (!list.getCompound(i).isEmpty()) {
                CompoundTag compound = list.getCompound(i);
                Vec3d pos = new Vec3d(compound.getDouble("x"), compound.getDouble("y"), compound.getDouble("z"));
                RegistryKey<World> dimension = RegistryKey.of(Registry.DIMENSION, new Identifier(compound.getString("dimension")));
                positions.put(compound.getUuid("uuid"), new Pair<>(pos, dimension));
                i++;
            }
        }
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        ListTag list = new ListTag();
        int i = 0;
        for (Map.Entry<UUID, Pair<Vec3d, RegistryKey<World>>> entry : positions.entrySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putUuid("uuid", entry.getKey());
            Vec3d pos = entry.getValue().getLeft();
            compound.putDouble("x", pos.x);
            compound.putDouble("y", pos.y);
            compound.putDouble("z", pos.z);
            compound.putString("dimension", entry.getValue().getRight().getValue().toString());
            list.add(i, compound);
        }
        tag.put("spectators", list);
        return tag;
    }
}

package ml.northwestwind.survivalspectator.data;

import com.google.common.collect.Maps;
import ml.northwestwind.survivalspectator.entity.FakePlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class PositionData extends PersistentState {
    private final Map<UUID, Pair<Vec3d, RegistryKey<World>>> positions = Maps.newHashMap();
    private final Map<UUID, UUID> playerPlaceholders = Maps.newHashMap();
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
        list = (ListTag) tag.get("fakes");
        if (list != null) {
            int i = 0;
            while (!list.getCompound(i).isEmpty()) {
                CompoundTag compound = list.getCompound(i);
                playerPlaceholders.put(compound.getUuid("uuid"), compound.getUuid("fake"));
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
            list.add(i++, compound);
        }
        tag.put("spectators", list);
        ListTag fakes = new ListTag();
        i = 0;
        for (Map.Entry<UUID, UUID> entry : playerPlaceholders.entrySet()) {
            CompoundTag compound = new CompoundTag();
            compound.putUuid("uuid", entry.getKey());
            compound.putUuid("fake", entry.getValue());
            fakes.add(i++, compound);
        }
        tag.put("fakes", fakes);
        return tag;
    }

    public boolean contains(UUID uuid) {
        return positions.containsKey(uuid) && playerPlaceholders.containsKey(uuid);
    }

    public void toSpectator(ServerPlayerEntity player) {
        player.setGameMode(GameMode.SPECTATOR);
        positions.put(player.getUuid(), new Pair<>(player.getPos(), player.world.getRegistryKey()));
        FakePlayerEntity fake = FakePlayerEntity.createFake(player.getEntityName(), player.getServer(), player.getX(), player.getY(), player.getZ(), player.yaw, player.pitch, player.world.getRegistryKey(), GameMode.SURVIVAL);
        if (fake != null) playerPlaceholders.put(player.getUuid(), fake.getUuid());
    }

    public void toSurvival(ServerPlayerEntity player) {
        player.setGameMode(GameMode.SURVIVAL);
        Vec3d pos = positions.get(player.getUuid()).getLeft();
        RegistryKey<World> dimension = positions.get(player.getUuid()).getRight();
        ServerPlayerEntity fake;
        if (!player.world.getRegistryKey().equals(dimension)) {
            ServerWorld world = ((ServerWorld) player.world).getServer().getWorld(dimension);
            if (world == null) return;
            fake = (ServerPlayerEntity) world.getEntity(playerPlaceholders.get(player.getUuid()));
            player.moveToWorld(world);
        } else {
            fake = (ServerPlayerEntity) player.getServerWorld().getEntity(playerPlaceholders.get(player.getUuid()));
            player.teleport(pos.x, pos.y, pos.z);
        }
        positions.remove(player.getUuid());
        playerPlaceholders.remove(player.getUuid());
        if (fake == null) return;
        if (fake.removed) player.kill();
        fake.kill();
    }

    public Vec3d getPlayerPos(UUID uuid) {
        return positions.get(uuid).getLeft();
    }

    @Nullable
    public UUID getPlayerByFake(UUID uuid) {
        return playerPlaceholders.entrySet().stream().filter(entry -> entry.getValue().equals(uuid)).findFirst().orElseGet(() -> new NullEntry<>(uuid)).getKey();
    }

    private static class NullEntry<K, V> implements Map.Entry<K, V> {
        private V value;

        public NullEntry(V value) {
            this.value = value;
        }

        @Override
        public K getKey() {
            return null;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            this.value = value;
            return value;
        }
    }
}

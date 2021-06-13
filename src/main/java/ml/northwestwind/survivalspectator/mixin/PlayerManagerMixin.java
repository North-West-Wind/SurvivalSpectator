package ml.northwestwind.survivalspectator.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.Unpooled;
import ml.northwestwind.survivalspectator.entity.FakePlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(value = PlayerManager.class, priority = 0)
public abstract class PlayerManagerMixin {

    @Shadow @Final private MinecraftServer server;

    @Shadow @Nullable public abstract NbtCompound loadPlayerData(ServerPlayerEntity player);

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract void sendCommandTree(ServerPlayerEntity player);

    @Shadow protected abstract void sendScoreboard(ServerScoreboard scoreboard, ServerPlayerEntity player);

    @Shadow public abstract void broadcastChatMessage(Text message, MessageType type, UUID senderUuid);

    @Shadow @Final private List<ServerPlayerEntity> players;

    @Shadow @Final private Map<UUID, ServerPlayerEntity> playerMap;

    @Shadow public abstract void sendToAll(Packet<?> packet);

    @Shadow public abstract void sendWorldInfo(ServerPlayerEntity player, ServerWorld world);

    @Shadow public abstract MinecraftServer getServer();

    @Shadow @Final private DynamicRegistryManager.Impl registryManager;

    @Shadow public abstract int getMaxPlayerCount();

    @Shadow private int viewDistance;

    /**
     * @author Fabric
     * @reason Allow fake
     */
    @Overwrite
    public void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player) {
        GameProfile gameProfile = player.getGameProfile();
        UserCache userCache = this.server.getUserCache();
        GameProfile gameProfile2 = userCache.getByUuid(gameProfile.getId());
        String string = gameProfile2 == null ? gameProfile.getName() : gameProfile2.getName();
        userCache.add(gameProfile);
        NbtCompound compoundTag = this.loadPlayerData(player);
        RegistryKey<World> var23;
        if (compoundTag != null) {
            DataResult<RegistryKey<World>> var10000 = DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, compoundTag.get("Dimension")));
            Logger var10001 = LOGGER;
            var10001.getClass();
            var23 = var10000.resultOrPartial(var10001::error).orElse(World.OVERWORLD);
        } else {
            var23 = World.OVERWORLD;
        }

        RegistryKey<World> registryKey = var23;
        ServerWorld serverWorld = this.server.getWorld(registryKey);
        ServerWorld serverWorld3;
        if (serverWorld == null) {
            LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", registryKey);
            serverWorld3 = this.server.getOverworld();
        } else {
            serverWorld3 = serverWorld;
        }

        player.setWorld(serverWorld3);
        player.interactionManager.setWorld((ServerWorld)player.world);
        String string2 = "local";
        if (connection.getAddress() != null) {
            string2 = connection.getAddress().toString();
        }

        LOGGER.info("{}[{}] logged in with entity id {} at ({}, {}, {})", player.getName().getString(), string2, player.getId(), player.getX(), player.getY(), player.getZ());
        WorldProperties worldProperties = serverWorld3.getLevelProperties();

        player.setGameMode(compoundTag);
        ServerPlayNetworkHandler serverPlayNetworkHandler = new ServerPlayNetworkHandler(this.server, connection, player);
        GameRules gameRules = serverWorld3.getGameRules();
        boolean bl = gameRules.getBoolean(GameRules.DO_IMMEDIATE_RESPAWN);
        boolean bl2 = gameRules.getBoolean(GameRules.REDUCED_DEBUG_INFO);
        serverPlayNetworkHandler.sendPacket(new GameJoinS2CPacket(player.getId(), player.interactionManager.getGameMode(), player.interactionManager.getPreviousGameMode(), BiomeAccess.hashSeed(serverWorld3.getSeed()), worldProperties.isHardcore(), this.server.getWorldRegistryKeys(), this.registryManager, serverWorld3.getDimension(), serverWorld3.getRegistryKey(), this.getMaxPlayerCount(), this.viewDistance, bl2, !bl, serverWorld3.isDebugWorld(), serverWorld3.isFlat()));
        serverPlayNetworkHandler.sendPacket(new CustomPayloadS2CPacket(CustomPayloadS2CPacket.BRAND, (new PacketByteBuf(Unpooled.buffer())).writeString(this.getServer().getServerModName())));
        serverPlayNetworkHandler.sendPacket(new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
        serverPlayNetworkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
        serverPlayNetworkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(player.getInventory().selectedSlot));
        serverPlayNetworkHandler.sendPacket(new SynchronizeRecipesS2CPacket(this.server.getRecipeManager().values()));
        serverPlayNetworkHandler.sendPacket(new SynchronizeTagsS2CPacket(this.server.getTagManager().toPacket(this.registryManager)));
        this.sendCommandTree(player);
        player.getStatHandler().updateStatSet();
        player.getRecipeBook().sendInitRecipesPacket(player);
        this.sendScoreboard(serverWorld3.getScoreboard(), player);
        this.server.forcePlayerSampleUpdate();
        TranslatableText mutableText2;
        if (player.getGameProfile().getName().equalsIgnoreCase(string)) {
            mutableText2 = new TranslatableText("multiplayer.player.joined", player.getDisplayName());
        } else {
            mutableText2 = new TranslatableText("multiplayer.player.joined.renamed", player.getDisplayName(), string);
        }

        if (!(player instanceof FakePlayerEntity)) this.broadcastChatMessage(mutableText2.formatted(Formatting.YELLOW), MessageType.SYSTEM, Util.NIL_UUID);
        serverPlayNetworkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        this.players.add(player);
        this.playerMap.put(player.getUuid(), player);
        this.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));

        for (ServerPlayerEntity serverPlayerEntity : this.players) {
            player.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, serverPlayerEntity));
        }

        serverWorld3.onPlayerConnected(player);
        this.server.getBossBarManager().onPlayerConnect(player);
        this.sendWorldInfo(player, serverWorld3);
        if (!this.server.getResourcePackUrl().isEmpty()) {
            player.sendResourcePackUrl(this.server.getResourcePackUrl(), this.server.getResourcePackHash(), false, new LiteralText(""));
        }

        for (StatusEffectInstance statusEffectInstance : player.getStatusEffects()) {
            serverPlayNetworkHandler.sendPacket(new EntityStatusEffectS2CPacket(player.getId(), statusEffectInstance));
        }

        if (compoundTag != null && compoundTag.contains("RootVehicle", 10)) {
            NbtCompound compoundTag2 = compoundTag.getCompound("RootVehicle");
            Entity entity = EntityType.loadEntityWithPassengers(compoundTag2.getCompound("Entity"), serverWorld3, (vehicle) -> !serverWorld3.tryLoadEntity(vehicle) ? null : vehicle);
            if (entity != null) {
                UUID uUID2;
                if (compoundTag2.containsUuid("Attach")) {
                    uUID2 = compoundTag2.getUuid("Attach");
                } else {
                    uUID2 = null;
                }

                Iterator<Entity> var21;
                Entity entity3;
                if (entity.getUuid().equals(uUID2)) {
                    player.startRiding(entity, true);
                } else {
                    var21 = entity.getPassengersDeep().iterator();

                    while(var21.hasNext()) {
                        entity3 = var21.next();
                        if (entity3.getUuid().equals(uUID2)) {
                            player.startRiding(entity3, true);
                            break;
                        }
                    }
                }

                if (!player.hasVehicle()) {
                    LOGGER.warn("Couldn't reattach entity to player");
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    var21 = entity.getPassengersDeep().iterator();

                    while(var21.hasNext()) {
                        entity3 = var21.next();
                        entity3.remove(Entity.RemovalReason.DISCARDED);
                    }
                }
            }
        }

        player.onSpawn();
    }
}

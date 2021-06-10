package ml.northwestwind.survivalspectator.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import ml.northwestwind.survivalspectator.data.PositionData;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.UUID;

// Thank you Carpet
@SuppressWarnings("EntityConstructor")
public class FakePlayerEntity extends ServerPlayerEntity
{
    public Runnable fixStartingPosition = () -> {};
    public boolean isAShadow;

    public static FakePlayerEntity createFake(String username, MinecraftServer server, double d0, double d1, double d2, double yaw, double pitch, RegistryKey<World> dimensionId, GameMode gamemode)
    {
        ServerWorld worldIn = server.getWorld(dimensionId);
        ServerPlayerInteractionManager interactionManagerIn = new ServerPlayerInteractionManager(worldIn);
        GameProfile profile = server.getUserCache().findByName(username);
        if (profile == null) return null;
        GameProfile gameprofile = new GameProfile(UUID.randomUUID(), username);
        if (profile.getProperties().containsKey("textures")) {
            gameprofile.getProperties().put("textures", (Property) profile.getProperties().get("textures"));
            gameprofile = SkullBlockEntity.loadProperties(gameprofile);
        }
        FakePlayerEntity instance = new FakePlayerEntity(server, worldIn, gameprofile, interactionManagerIn, false);
        instance.fixStartingPosition = () -> instance.refreshPositionAndAngles(d0, d1, d2, (float) yaw, (float) pitch);
        server.getPlayerManager().onPlayerConnect(new FakeNetworkManager(NetworkSide.SERVERBOUND), instance);
        instance.teleport(worldIn, d0, d1, d2, (float)yaw, (float)pitch);
        instance.setHealth(20.0F);
        instance.removed = false;
        instance.stepHeight = 0.6F;
        interactionManagerIn.setGameMode(gamemode);
        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(instance, (byte) (instance.headYaw * 256 / 360)), dimensionId);//instance.dimension);
        server.getPlayerManager().sendToDimension(new EntityPositionS2CPacket(instance), dimensionId);//instance.dimension);
        instance.getServerWorld().getChunkManager().updateCameraPosition(instance);
        instance.dataTracker.set(PLAYER_MODEL_PARTS, (byte) 0x7f); // show all model layers (incl. capes)
        return instance;
    }

    private FakePlayerEntity(MinecraftServer server, ServerWorld worldIn, GameProfile profile, ServerPlayerInteractionManager interactionManagerIn, boolean shadow)
    {
        super(server, worldIn, profile, interactionManagerIn);
        isAShadow = shadow;
    }

    @Override
    protected void onEquipStack(ItemStack stack)
    {
        if (!isUsingItem()) super.onEquipStack(stack);
    }

    @Override
    public void tick()
    {
        if (this.getServer().getTicks() % 10 == 0)
        {
            this.networkHandler.syncWithPlayerPosition();
            this.getServerWorld().getChunkManager().updateCameraPosition(this);
        }
        super.tick();
        this.playerTick();
    }

    private void shakeOff()
    {
        if (getVehicle() instanceof PlayerEntity) stopRiding();
        for (Entity passenger : getPassengersDeep())
        {
            if (passenger instanceof PlayerEntity) passenger.stopRiding();
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean sup = super.damage(source, amount);
        if (sup) {
            PositionData data = PositionData.get(this.getServerWorld());
            UUID uuid = data.getPlayerByFake(this.getUuid());
            if (uuid != null) {
                Entity entity = this.getServerWorld().getEntity(uuid);
                if (entity instanceof ServerPlayerEntity) data.toSurvival((ServerPlayerEntity) entity);
            }
        }
        return sup;
    }

    @Override
    public void onDeath(DamageSource cause)
    {
        shakeOff();
        kill();
        PositionData data = PositionData.get(this.getServerWorld());
        UUID uuid = data.getPlayerByFake(this.getUuid());
        if (uuid != null) {
            Entity entity = this.getServerWorld().getEntity(uuid);
            if (entity instanceof ServerPlayerEntity) {
                data.toSurvival((ServerPlayerEntity) entity);
                ((ServerPlayerEntity) entity).onDeath(cause);
            }
        }
    }

    @Override
    public void kill()
    {
        kill(new LiteralText("Killed"));
    }

    public void kill(Text reason)
    {
        shakeOff();
        this.server.send(new ServerTask(this.server.getTicks(), () -> this.networkHandler.onDisconnected(reason)));
    }

    @Override
    public String getIp()
    {
        return "127.0.0.1";
    }
}
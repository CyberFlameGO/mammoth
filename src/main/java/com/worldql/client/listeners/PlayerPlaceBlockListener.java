package com.worldql.client.listeners;

import com.worldql.client.WorldQLClient;
import com.worldql.client.events.PlayerHoldEvent;
import com.worldql.client.serialization.*;
import com.worldql.client.serialization.Record;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import zmq.ZMQ;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class PlayerPlaceBlockListener implements Listener {

    @EventHandler
    public void onPlayerPlaceBlockEvent(BlockPlaceEvent e) {
        Record placedBlock = new Record(
                UUID.nameUUIDFromBytes(e.getBlockPlaced().getLocation().toString().getBytes(StandardCharsets.UTF_8)),
                new Vec3D(e.getBlockPlaced().getLocation()),
                e.getBlockPlaced().getWorld().getName(),
                e.getBlockPlaced().getBlockData().getAsString(),
                null
        );
        // TODO: Handle compound blocks (beds, doors) and joined blocks (fences, glass panes)

        Message message = new Message(
                Instruction.RecordCreate,
                WorldQLClient.worldQLClientId,
                e.getPlayer().getWorld().getName(),
                Replication.ExceptSelf,
                // This field isn't really used since the Record also contains the position
                // of the changed block(s).
                new Vec3D(e.getBlockPlaced().getLocation()),
                List.of(placedBlock),
                null,
                "MinecraftBlockUpdate",
                null
        );
        WorldQLClient.getPluginInstance().getPushSocket().send(message.encode(), ZMQ.ZMQ_DONTWAIT);

        // send a LocalMessage instruction with the same information so that clients can get an update on the chunk.
        Message localMessage = message.withInstruction(Instruction.LocalMessage);
        WorldQLClient.getPluginInstance().getPushSocket().send(localMessage.encode(), ZMQ.ZMQ_DONTWAIT);



        // Update hand visual if they ran out of blocks in their hand.
        if (e.getPlayer().getInventory().getItemInMainHand().getAmount() -1 <= 0)
            Bukkit.getPluginManager().callEvent(new PlayerHoldEvent(e.getPlayer(), new ItemStack(Material.AIR), PlayerHoldEvent.HandType.MAINHAND));
    }
}
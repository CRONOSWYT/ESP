package com.ik.blockesp.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class ClientESP {
    public static boolean enabled = true;
    public static int radius = 32;
    public static boolean seeThroughEnabled = true;
    private static final Map<Block, ClientConfig.BlockStyle> styles = new HashMap<>();
    private static final Map<Item, ClientConfig.BlockStyle> itemStyles = new HashMap<>();
    public static final java.util.concurrent.CopyOnWriteArrayList<BlockPos> targetPositions = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final java.util.concurrent.CopyOnWriteArrayList<ItemEntity> targetItems = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static int tickCounter = 0;
    private static final int scanInterval = 20; // ~1s, para actualizar más rápido
    // Persistencia simple para la GUI
    public static String lastQuery = "";
    public static ResourceLocation lastSelectedBlockId = null;

    public static void reloadTargets() {
        ClientConfig.ConfigData cfg = ClientConfig.loadConfig();
        styles.clear();
        itemStyles.clear();
        radius = Math.max(8, cfg.radius);
        seeThroughEnabled = cfg.seeThrough;
        // Autodetección: poblar estilos con todos los bloques del registro (disabled por defecto)
        for (Block b : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            ClientConfig.BlockStyle st = new ClientConfig.BlockStyle();
            st.enabled = false;
            styles.put(b, st);
        }
        // Aplicar overrides desde config
        for (Map.Entry<String, ClientConfig.BlockStyle> e : cfg.blocks.entrySet()) {
            try {
                var opt = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(e.getKey()));
                Block b = opt.map(h -> h.value()).orElse(null);
                if (b != null) styles.put(b, e.getValue());
            } catch (Exception ignored) {}
        }

        // Autodetección: poblar estilos de items (disabled por defecto)
        for (Item it : BuiltInRegistries.ITEM) {
            ClientConfig.BlockStyle st = new ClientConfig.BlockStyle();
            st.enabled = false;
            itemStyles.put(it, st);
        }
        for (Map.Entry<String, ClientConfig.BlockStyle> e : cfg.items.entrySet()) {
            try {
                var opt = BuiltInRegistries.ITEM.get(ResourceLocation.parse(e.getKey()));
                Item it = opt.map(h -> h.value()).orElse(null);
                if (it != null) itemStyles.put(it, e.getValue());
            } catch (Exception ignored) {}
        }
    }

    public static void onClientTick() {
        if (!enabled) return;
        tickCounter++;
        if (tickCounter % scanInterval == 0) {
            rebuildPositions();
            rebuildItemTargets();
        }
    }

    private static void rebuildPositions() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;
        BlockPos center = mc.player.blockPosition();
        targetPositions.clear();
        int r = radius;
        int minY = Math.max(-64, center.getY() - r);
        int maxY = Math.min(320, center.getY() + r);

        for (int y = minY; y <= maxY; y++) {
            for (int x = center.getX() - r; x <= center.getX() + r; x++) {
                for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    Block b = state.getBlock();
                    ClientConfig.BlockStyle st = styles.get(b);
                    if (st != null && st.enabled) {
                        targetPositions.add(pos);
                    }
                }
            }
        }
    }

    private static void rebuildItemTargets() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null) return;
        BlockPos center = mc.player.blockPosition();
        int r = radius;
        AABB box = new AABB(center).inflate(r);
        targetItems.clear();
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, box)) {
            Item item = ie.getItem().getItem();
            ClientConfig.BlockStyle st = itemStyles.get(item);
            if (st != null && st.enabled) {
                targetItems.add(ie);
            }
        }
    }

    public static ClientConfig.BlockStyle getStyle(Block b) {
        return styles.get(b);
    }

    public static Map<Block, ClientConfig.BlockStyle> getStyles() {
        return styles;
    }

    public static ClientConfig.BlockStyle getItemStyle(Item item) {
        return itemStyles.get(item);
    }

    public static Map<Item, ClientConfig.BlockStyle> getItemStyles() {
        return itemStyles;
    }

    public static void onChunkLoaded() {
        if (!enabled) return;
        rebuildPositions();
        rebuildItemTargets();
    }
}

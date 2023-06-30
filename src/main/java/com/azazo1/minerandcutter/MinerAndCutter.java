package com.azazo1.minerandcutter;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;

public final class MinerAndCutter extends JavaPlugin implements Listener {
    public final DoubleClickChecker doubleClickCheckerOfMiner = new DoubleClickChecker(this);
    public final DoubleClickChecker doubleClickCheckerOfTree = new DoubleClickChecker(this);
    public static final HashSet<Material> mineTypes = new HashSet<>() {{
        add(Material.COAL_ORE);
        add(Material.DEEPSLATE_COAL_ORE);
        add(Material.COPPER_ORE);
        add(Material.DEEPSLATE_COPPER_ORE);
        add(Material.EMERALD_ORE);
        add(Material.DEEPSLATE_EMERALD_ORE);
        add(Material.DIAMOND_ORE);
        add(Material.DEEPSLATE_DIAMOND_ORE);
        add(Material.LAPIS_ORE);
        add(Material.DEEPSLATE_LAPIS_ORE);
        add(Material.IRON_ORE);
        add(Material.DEEPSLATE_IRON_ORE);
        add(Material.GOLD_ORE);
        add(Material.DEEPSLATE_GOLD_ORE);
        add(Material.REDSTONE_ORE);
        add(Material.DEEPSLATE_REDSTONE_ORE);
        add(Material.ANCIENT_DEBRIS);
        add(Material.NETHER_GOLD_ORE);
        add(Material.NETHER_QUARTZ_ORE);
        add(Material.RAW_GOLD);
        add(Material.RAW_IRON);
        add(Material.RAW_COPPER);
    }};
    public static final HashSet<Material> treeTypes = new HashSet<>() {{
        add(Material.OAK_LOG);
        add(Material.SPRUCE_LOG);
        add(Material.BIRCH_LOG);
        add(Material.JUNGLE_LOG);
        add(Material.ACACIA_LOG);
        add(Material.DARK_OAK_LOG);
        add(Material.MANGROVE_LOG);
        add(Material.CHERRY_LOG);
    }};
    public static final HashSet<Material> pickaxeTypes = new HashSet<>() {{
        add(Material.WOODEN_PICKAXE);
        add(Material.STONE_PICKAXE);
        add(Material.IRON_PICKAXE);
        add(Material.DIAMOND_PICKAXE);
        add(Material.GOLDEN_PICKAXE);
        add(Material.NETHERITE_PICKAXE);
    }};
    public static final HashSet<Material> axeTypes = new HashSet<>() {{
        add(Material.WOODEN_AXE);
        add(Material.STONE_AXE);
        add(Material.IRON_AXE);
        add(Material.DIAMOND_AXE);
        add(Material.GOLDEN_AXE);
        add(Material.NETHERITE_AXE);
    }};

    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // 连锁挖矿  连锁砍树
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock == null) { // 点击的不是方块
                return;
            }
            Material type = clickedBlock.getType();

            if (isInMind(type)) {
                if (isInPickaxe(player.getInventory().getItemInMainHand().getType()) && doubleClickCheckerOfMiner.checkDoubleClick(player)) { // 要在内层 确保玩家拿着镐子双击且双击的是对应方块
                    // 挖矿
                    digBlocksInOneTime(clickedBlock, player);
                }
            } else if (isInTree(type)) {
                if (isInAxe(player.getInventory().getItemInMainHand().getType()) && doubleClickCheckerOfTree.checkDoubleClick(player)) {
                    // 挖树
                    digBlocksInOneTime(clickedBlock, player);
                }
            }
        }

    }

    /**
     * 连锁采集某种方块
     *
     * @param targetBlock 要被采集的起始方块, 连锁采集与之相邻的方块
     * @param player      采集的玩家, 采集后的物品将直接送至该玩家处
     */
    private void digBlocksInOneTime(@NotNull Block targetBlock, @NotNull Player player) {
        ItemStack is = player.getInventory().getItemInMainHand();
        ArrayList<Block> blocks = searchBlocks(targetBlock, null); // 搜寻相邻同类方块
        // 检查工具耐久值是否足够
        int damage = ((Damageable) is.getItemMeta()).getDamage();
        int size = blocks.size();
        if (damage < size) {
            player.sendMessage(Component.text("你所持的工具耐久不足! (需要: %d, 你的工具: %d)".formatted(size, damage)));
        } else {
            targetBlock.breakNaturally(is);
            player.sendMessage(Component.text("挖掘了 %d 个方块, 工具剩余耐久: %d".formatted(size, damage)));
        }
    }

    /**
     * 搜寻与这个方块相邻的所有同类方块
     */
    private @NotNull ArrayList<Block> searchBlocks(@NotNull Block targetBlock, @Nullable ArrayList<Block> discoveredBlocks) {
        if (discoveredBlocks == null) {
            discoveredBlocks = new ArrayList<>();
        } else if (discoveredBlocks.contains(targetBlock)) {
            return discoveredBlocks; // 终止搜索
        }
        discoveredBlocks.add(targetBlock);
        Location loc = targetBlock.getLocation();
        Location locX = loc.clone();
        locX.setX(loc.getX() + 1);
        Location locNX = loc.clone();
        locNX.setX(loc.getX() - 1);
        Location locY = loc.clone();
        locY.setY(loc.getY() + 1);
        Location locNY = loc.clone();
        locNY.setY(loc.getY() - 1);
        Location locZ = loc.clone();
        locZ.setZ(loc.getZ() + 1);
        Location locNZ = loc.clone();
        locNZ.setZ(loc.getZ() - 1);
        for (Location _loc : new Location[]{locX, locNX, locY, locNY, locZ, locNZ}) {
            if (_loc.getBlock().getType().equals(targetBlock.getType())) {
                searchBlocks(targetBlock, discoveredBlocks);
                getLogger().info("discover block: %.1f, %.1f, %.1f".formatted(_loc.getX(), _loc.getY(), _loc.getZ()));
            }
        }
        return discoveredBlocks;
    }

    /**
     * 查询某个方块是否是矿物
     */
    private boolean isInMind(Material material) {
        return mineTypes.contains(material);
    }

    /**
     * 查询某个物品是否是镐子
     */
    private boolean isInPickaxe(Material material) {
        return pickaxeTypes.contains(material);
    }


    /**
     * 查询某个方块是否是树木原木
     */
    private boolean isInTree(Material material) {
        return treeTypes.contains(material);
    }

    /**
     * 查询某个物品是否是斧头
     */
    private boolean isInAxe(Material material) {
        return axeTypes.contains(material);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

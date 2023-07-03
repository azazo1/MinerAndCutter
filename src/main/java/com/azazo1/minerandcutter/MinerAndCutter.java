package com.azazo1.minerandcutter;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
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
import java.util.*;

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
    public static final HashMap<Material, Integer> pickaxeTypes = new HashMap<>() {{ // 镐子和最大耐久
        put(Material.WOODEN_PICKAXE, 59);
        put(Material.STONE_PICKAXE, 131);
        put(Material.IRON_PICKAXE, 250);
        put(Material.DIAMOND_PICKAXE, 1561);
        put(Material.GOLDEN_PICKAXE, 32);
        put(Material.NETHERITE_PICKAXE, 2031);
    }};
    public static final HashMap<Material, Integer> axeTypes = new HashMap<>() {{ // 斧头和最大耐久
        put(Material.WOODEN_AXE, 59);
        put(Material.STONE_AXE, 131);
        put(Material.IRON_AXE, 250);
        put(Material.DIAMOND_AXE, 1561);
        put(Material.GOLDEN_AXE, 32);
        put(Material.NETHERITE_AXE, 2031);
    }};
    public static final HashSet<Material> leafTypes = new HashSet<>() {{
        add(Material.OAK_LEAVES);
        add(Material.SPRUCE_LEAVES);
        add(Material.BIRCH_LEAVES);
        add(Material.JUNGLE_LEAVES);
        add(Material.ACACIA_LEAVES);
        add(Material.DARK_OAK_LEAVES);
        add(Material.MANGROVE_LEAVES);
        add(Material.CHERRY_LEAVES);
        add(Material.FLOWERING_AZALEA_LEAVES);
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
            Material handItem = player.getInventory().getItemInMainHand().getType();
            if (isInMind(type) && isInPickaxe(handItem)) {
                event.setCancelled(true);
                if (doubleClickCheckerOfMiner.checkDoubleClick(player)) { // 要在内层 确保玩家拿着镐子双击且双击的是对应方块
                    // 挖矿
                    digBlocksInOneTime(clickedBlock, player, pickaxeTypes.get(handItem));
                }
            } else if (isInTree(type) && isInAxe(handItem)) {
                event.setCancelled(true);
                if (doubleClickCheckerOfTree.checkDoubleClick(player)) {
                    // 挖树
                    digBlocksInOneTime(clickedBlock, player, axeTypes.get(handItem));
                }
            }
        }

    }

    /**
     * 连锁采集某种方块
     *
     * @param targetBlock  要被采集的起始方块, 连锁采集与之相邻的方块
     * @param player       采集的玩家, 采集后的物品将直接送至该玩家处
     * @param maxEndurance 玩家手上采集工具的最大耐久
     */
    private void digBlocksInOneTime(@NotNull Block targetBlock, @NotNull Player player, int maxEndurance) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        // 检查该工具是否能挖掘该类方块
        Collection<ItemStack> drops = targetBlock.getDrops(itemInMainHand);
        if (drops.size() == 0) {
            player.sendMessage(Component.text("你的工具等级不足以挖掘该矿物!"));
            return;
        }
        LinkedList<Block> blocks; // 搜寻相邻同类方块
        if (isInMind(targetBlock.getType())) {
            blocks = searchMine(targetBlock, null);
        } else if (isInTree(targetBlock.getType())) {
            blocks = searchTree(targetBlock);
        } else {
            player.sendMessage(Component.text("你正在进行无效的尝试"));
            return;
        }
        // 获取背包内同种类同级别工具的总耐久
        List<ItemStack> sameTypeItems = getItemsOfAPlayer(player, itemStack -> itemStack != null && itemStack.getType() == itemInMainHand.getType());
        ArrayList<ItemStack> sameTypeItemsArrayList = new ArrayList<>(sameTypeItems);
        int totalEndurance = sameTypeItemsArrayList.stream().mapToInt(is1 -> {
            Damageable itemMeta = (Damageable) is1.getItemMeta();
            int damage = itemMeta.getDamage();
            return maxEndurance - damage;
        }).sum();
        // 检查工具耐久值是否足够
        int totalNeed = blocks.size();
        if (totalEndurance < blocks.size()) {
            player.sendMessage(Component.text("你所拥有的同类工具耐久不足! (需要: %d, 你的背包内同类工具总可用耐久: %d)".formatted(totalNeed, totalEndurance)));
            return;
        }

        sameTypeItemsArrayList.remove(itemInMainHand);
        sameTypeItemsArrayList.add(0, itemInMainHand); // 手上的放到首位
        for (ItemStack is : sameTypeItemsArrayList) { // 逐个工具尝试挖掘
            int restNeed = blocks.size(); // 还需要挖掘的方块数量
            if (restNeed == 0) {
                break;
            }

            Damageable itemMeta = (Damageable) is.getItemMeta();
            int damage = itemMeta.getDamage();
            int endurance = maxEndurance - damage; // 该工具剩余耐久
            if (restNeed < endurance) { // 这个工具可承担剩下的挖掘任务
                while (!blocks.isEmpty()) {
                    Block discoveredBlock = blocks.pop();
                    discoveredBlock.breakNaturally(itemInMainHand);
                    Collection<Item> dropItems = player.getWorld().getNearbyEntitiesByType(Item.class, discoveredBlock.getLocation(), 2);
                    dropItems.forEach(item -> {
                        item.teleport(player);
                        item.setPickupDelay(0);
                    });
                }
                damage += restNeed;
                itemMeta.setDamage(damage);
                is.setItemMeta(itemMeta);
            } else { // 该工具不能独立承担剩下的挖掘任务，该工具被挖破后剩下的同类工具出马
                for (int i = 0; i < endurance; i++) {
                    Block discoveredBlock = blocks.pop();
                    discoveredBlock.breakNaturally(itemInMainHand);
                    Collection<Item> dropItems = player.getWorld().getNearbyEntitiesByType(Item.class, discoveredBlock.getLocation(), 2);
                    dropItems.forEach(item -> {
                        item.teleport(player);
                        item.setPickupDelay(0);
                    });
                }
                player.getInventory().removeItem(is);
                player.playSound(player, Sound.ENTITY_ITEM_BREAK, 1, 1);
            }
        }
        player.sendMessage(Component.text("挖掘了 %d 个方块, 背包内同类工具剩余耐久: %d".formatted(totalNeed, totalEndurance - totalNeed)));
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    private List<ItemStack> getItemsOfAPlayer(Player player, ItemStackFilter filter) {
        return Arrays.stream(player.getInventory().getContents()).filter(filter::filter).toList();
    }

    /**
     * 搜寻与这个方块相邻的所有同类矿物方块
     */
    private @NotNull LinkedList<Block> searchMine(@NotNull Block targetBlock, @Nullable LinkedList<Block> discoveredBlocks) {
        if (discoveredBlocks == null) {
            discoveredBlocks = new LinkedList<>();
        } else if (discoveredBlocks.contains(targetBlock)) {
            return discoveredBlocks; // 终止搜索
        }
        discoveredBlocks.add(targetBlock);
        Block blockX = targetBlock.getRelative(1, 0, 0);
        Block blockNX = targetBlock.getRelative(-1, 0, 0);
        Block blockZ = targetBlock.getRelative(0, 0, 1);
        Block blockNZ = targetBlock.getRelative(0, 0, -1);
        Block blockY = targetBlock.getRelative(0, 1, 0);
        Block blockNY = targetBlock.getRelative(0, -1, 0);
        for (Block relativeBlock : new Block[]{blockX, blockNX, blockZ, blockNZ, blockY, blockNY}) {
            if (relativeBlock.getType() == targetBlock.getType()) {
                searchMine(relativeBlock, discoveredBlocks);
            }
        }
        return discoveredBlocks;
    }

    /**
     * BFS 搜寻一颗树内的所有树木方块
     */
    private @NotNull LinkedList<Block> searchTree(@NotNull Block targetBlock) {
        HashSet<Block> discoveredBlocks = new HashSet<>();// 初始化结果列表
        HashSet<Block> searchedLeaves = new HashSet<>();

        LinkedList<Block> blocks = new LinkedList<>() {{
            add(targetBlock.getRelative(1, 0, 0));
            add(targetBlock.getRelative(-1, 0, 0));
            add(targetBlock.getRelative(0, 0, 1));
            add(targetBlock.getRelative(0, 0, -1));
            add(targetBlock.getRelative(0, 1, 0));
            add(targetBlock.getRelative(0, -1, 0));
        }};

        discoveredBlocks.add(targetBlock);

        while (blocks.size() > 0) {
            Block selected = blocks.pop();
            if (selected.getType() == targetBlock.getType() // 与 结果 中的方块类型相同，即判断是否是树木方块
                    || isInLeaf(selected.getType())) { // 树叶也要搜索
                if (discoveredBlocks.contains(selected) || searchedLeaves.contains(selected)) { // 已经搜索过
                    continue;
                }
                if (selected.getType() == targetBlock.getType()) {
                    discoveredBlocks.add(selected);
                } else { // 到这里可以断言这必是树叶
                    searchedLeaves.add(selected);
                }
                blocks.add(selected.getRelative(1, 0, 0));
                blocks.add(selected.getRelative(-1, 0, 0));
                blocks.add(selected.getRelative(0, 0, 1));
                blocks.add(selected.getRelative(0, 0, -1));
                blocks.add(selected.getRelative(0, 1, 0));
                blocks.add(selected.getRelative(0, -1, 0));
            }
        }
        return new LinkedList<>(discoveredBlocks);
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
        return pickaxeTypes.containsKey(material);
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
        return axeTypes.containsKey(material);
    }

    /**
     * 查询某个方块是否是树叶
     */
    private boolean isInLeaf(Material material) {
        return leafTypes.contains(material);
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

    public interface ItemStackFilter {
        boolean filter(ItemStack is);
    }
}

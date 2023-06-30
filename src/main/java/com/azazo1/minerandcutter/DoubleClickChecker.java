package com.azazo1.minerandcutter;

import org.bukkit.entity.Player;

import java.util.HashMap;

/**
 * 检查玩家是否双击
 * 注意时间都是游戏内时间
 */
public class DoubleClickChecker {
    public final MinerAndCutter plugin;
    protected HashMap<Player, Long> lastClickTime = new HashMap<>();

    public static final int DOUBLE_CLICK_MAX_INTERVAL_TICK = 3;

    public DoubleClickChecker(MinerAndCutter plugin) {
        this.plugin = plugin;
    }

    public boolean checkDoubleClick(Player player) {
        if (lastClickTime.containsKey(player)) {
            long singleLastClickTime = lastClickTime.get(player);
            long nowTime = player.getWorld().getTime();
            if (nowTime - singleLastClickTime < DOUBLE_CLICK_MAX_INTERVAL_TICK) {
                lastClickTime.remove(player); // 防止第二次双击
                return true;
            } else { // 距离上次单击很久了, 重置首击时间
                lastClickTime.put(player, nowTime);
                return false;
            }
        } else {
            lastClickTime.put(player, player.getWorld().getTime());
            return false;
        }
    }
}

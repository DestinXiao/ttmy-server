package com.maple.game.osee.entity;

/**
 * 物品数据结构
 */
public class ItemData {

    /**
     * 物品id
     */
    private int itemId;

    /**
     * 数量
     */
    private long count;

    public ItemData() {
    }

    public ItemData(int itemId, long count) {
        this.itemId = itemId;
        this.count = count;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

}

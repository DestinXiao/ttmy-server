package com.maple.game.osee.dao.data.entity;

import org.apache.ibatis.annotations.Mapper;

import com.maple.database.data.DbEntity;

/**
 * 1688 奖券商品类
 */
@Mapper
public class OseeLotteryShopEntity extends DbEntity {

    private static final long serialVersionUID = 5183966291237509175L;

    /**
     * 序号
     */
    private int index;

    /**
     * 类型 1:实物 2:钻石 3:金币
     */
    private int type;

    /**
     * 发货类型 1-实时兑换 2-手动发货 3-自动发卡
     */
    private int sendType;

    /**
     * 库存数量
     */
    private int stock;

    /**
     * 数量
     */
    private long count;

    /**
     * 名称
     */
    private String name;

    /**
     * 消耗奖券数量
     */
    private long cost;

    /**
     * 奖品图片
     */
    private String img;

    /**
     * 总数量
     */
    private int size;

    /**
     * 已使用数量
     */
    private int usedSize;

    /**
     * 更新类型 0:无限制 1:每日更新 2:每周更新 3:每月更新
     */
    private int refreshType;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(int restSize) {
        this.usedSize = restSize;
    }

    public int getRefreshType() {
        return refreshType;
    }

    public void setRefreshType(int refreshType) {
        this.refreshType = refreshType;
    }

    public int getSendType() {
        return sendType;
    }

    public void setSendType(int sendType) {
        this.sendType = sendType;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}

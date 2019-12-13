package com.maple.game.osee.dao.data.entity;

import com.alibaba.fastjson.JSON;
import com.maple.database.data.DbEntity;
import com.maple.game.osee.entity.ItemData;

/**
 * 游戏内消息/邮件数据实体类
 *
 * @author Junlong
 */
public class MessageEntity extends DbEntity {
    private static final long serialVersionUID = 4104432655046923294L;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 发消息玩家ID
     */
    private Long fromId = 0L;

    /**
     * 接收消息玩家ID
     */
    private Long toId = 0L;

    /**
     * 附件信息(json格式,实际存入数据库内的数据)
     */
    private String itemsJson;

    /**
     * 消息附件物品
     */
    private ItemData[] items;

    /**
     * 是否已读
     */
    private Boolean read = false;

    /**
     * 是否领取附件
     */
    private Boolean receive = false;

    /**
     * 数据状态 0-正常 1-删除
     */
    private Integer state = 0;

    public Long getToId() {
        return toId;
    }

    public void setToId(Long toId) {
        this.toId = toId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Boolean getReceive() {
        return receive;
    }

    public void setReceive(Boolean receive) {
        this.receive = receive;
    }

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public String getItemsJson() {
        if (this.items != null) {
            this.itemsJson = JSON.toJSONString(this.items);
        }
        return itemsJson;
    }

    public void setItemsJson(String itemsJson) {
        if (itemsJson != null && !itemsJson.equals("")) {
            // 数据库给该属性赋值之后就直接转换为数组格式数据
            ItemData[] itemData = JSON.parseObject(itemsJson, ItemData[].class);
            this.setItems(itemData);
        }
        this.itemsJson = itemsJson;
    }

    public ItemData[] getItems() {
        return items;
    }

    public void setItems(ItemData[] items) {
        this.items = items;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }
}

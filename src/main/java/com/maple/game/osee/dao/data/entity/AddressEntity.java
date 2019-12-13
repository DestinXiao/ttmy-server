package com.maple.game.osee.dao.data.entity;

import com.maple.database.data.DbEntity;

/**
 * 玩家收货地址实体
 *
 * @author Junlong
 */
public class AddressEntity extends DbEntity {
    private static final long serialVersionUID = 4397627979435767401L;

    /**
     * 玩家ID
     */
    private Long playerId;

    /**
     * 收货人
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 收货地址
     */
    private String address;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

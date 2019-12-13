package com.maple.game.osee.entity.fightten.config;

import java.io.Serializable;
import java.util.List;

/**
 * 拼十场次属性配置
 */
public class FieldConfig implements Serializable {
    private static final long serialVersionUID = -7951140390306186728L;

    /**
     * 配置列表
     */
    private List<Config> configs;

    public static class Config {
        /**
         * 入场最低金币限制
         */
        private Long enterMoney;

        /**
         * 最高下注限制
         */
        private Long maxBetMoney;

        /**
         * 场次类型：0-初、1-中、2-高
         */
        private Integer type;

        public Long getEnterMoney() {
            return enterMoney;
        }

        public void setEnterMoney(Long enterMoney) {
            this.enterMoney = enterMoney;
        }

        public Long getMaxBetMoney() {
            return maxBetMoney;
        }

        public void setMaxBetMoney(Long maxBetMoney) {
            this.maxBetMoney = maxBetMoney;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "Config{" +
                    "enterMoney=" + enterMoney +
                    ", maxBetMoney=" + maxBetMoney +
                    ", type=" + type +
                    '}';
        }
    }
    public List<Config> getConfigs() {
        return configs;
    }

    public void setConfigs(List<Config> configs) {
        this.configs = configs;
    }
}

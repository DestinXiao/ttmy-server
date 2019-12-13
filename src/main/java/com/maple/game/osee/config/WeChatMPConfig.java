package com.maple.game.osee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信公众号配置信息
 *
 * @author Junlong
 */
@Component
@ConfigurationProperties(prefix = "wechat.mp")
public class WeChatMPConfig {

    private String appid;

    private String secret;

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}

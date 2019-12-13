package com.maple.game.osee.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.maple.engine.container.DataContainer;
import com.maple.game.osee.entity.lobby.csv.RobotNameConfig;
import com.maple.network.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 基础机器人管理类
 */
public class BaseRobotManager {

    private static Logger logger = LoggerFactory.getLogger(BaseRobotManager.class);

    /**
     * 机器人id创建对象
     */
    private static AtomicLong robotIdCreator = new AtomicLong(10000);

    /**
     * 获取新的机器人id
     */
    public static Long getNewRobotId() {
        return robotIdCreator.incrementAndGet();
    }

    /**
     * 获取随机机器人昵称
     */
    public static String getRobotName() {
        return DataContainer.getRandomData(RobotNameConfig.class).getName();
    }

//    /**
//     * 机器人头像图片类别
//     */
//    private static final String[] SORT = {"男", "女", "动漫男", "动漫女"};

    /**
     * 获取随机头像地址
     */
    public static String getRandomHeadUrl() {
        try {
            String headUrl = HttpUtil.doGet("https://api.uomg.com/api/rand.avatar?format=json"); // &sort=" + SORT[ThreadLocalRandom.current().nextInt(0, SORT.length)]);
            if (StringUtils.isEmpty(headUrl)) {
                return "";
            }
            JSONObject object = JSON.parseObject(headUrl);
            if (object == null) {
                return "";
            }
            int code = object.getIntValue("code");
            if (code != 1) {
                return "";
            }
            return object.getString("imgurl");
        } catch (Exception e) {
            logger.error("获取随机机器人头像链接出错:{}", e.getMessage());
            return "";
        }
    }

}

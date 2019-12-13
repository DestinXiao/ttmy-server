package com.maple.game.osee.controller.fightten.gm;

import com.maple.engine.anotation.GmController;
import com.maple.engine.anotation.GmHandler;
import com.maple.engine.manager.GsonManager;
import com.maple.game.osee.entity.gm.CommonResponse;
import com.maple.game.osee.manager.fightten.FightTenManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 拼十后台交互接口
 */
@GmController
public class GmFigthTenController {

    @Autowired
    private FightTenManager fightTenManager;

    /**
     * GM默认检查器
     */
    public String checker(Method taskMethod, Object param) {
        try {
            return GsonManager.gson.toJson(taskMethod.invoke(this, param));
        } catch (Exception e) {
            e.printStackTrace();
            return GsonManager.gson.toJson(new CommonResponse("ERROR_UNKNOWN", "操作异常！"));
        }
    }

    /**
     * 设置拼十场次属性
     * <p>
     * 传入参数形式
     * {
     * "configs":[
     * {"type":0,"maxBetMoney":0},
     * {"type":1,"maxBetMoney":0},
     * {"type":2,"maxBetMoney":0}
     * ]
     * }
     */
    @GmHandler(key = "/osee/fightten/field_config")
    public CommonResponse setFieldConfig(Map<String, Object> param) {
        return fightTenManager.gmSetFieldConfig(param);
    }

    /**
     * 设置拼十机器人属性
     * <p>
     * 传入参数形式
     * {"useRobot":true,"robotNum":3,"refreshTimeRangeBegin":0,"refreshTimeRangeEnd":10}
     */
    @GmHandler(key = "/osee/fightten/robot_config")
    public CommonResponse setRobotConfig(Map<String, Object> param) {
        return fightTenManager.gmSetRobotConfig(param);
    }

    /**
     * 机器人金币记录清空
     */
    @GmHandler(key = "/osee/fightten/robot/money/reset")
    public CommonResponse resetRobotMoney(Map<String, Object> param) {
        return fightTenManager.gmResetRobotMoney();
    }
}

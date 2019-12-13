package com.maple.game.osee.controller.pig;

import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.manager.pig.GoldenPigManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.goldenpig.TtmyGoldenPig;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

/**
 * 砸金猪控制类
 */
@AppController
public class GoldenPigController {

    @Autowired
    private GoldenPigManager goldenPigManager;

    /**
     * 默认检查器
     */
    public void checker(Method taskMethod, Message message, ServerUser user, Long exp) {
        try {
            taskMethod.invoke(this, message, user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 砸金猪请求
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_GOLDEN_PIG_BREAK_REQUEST_VALUE)
    public void pigBreak(TtmyGoldenPig.GoldenPigBreakRequest request, ServerUser user) {
        goldenPigManager.pigBreak(user, request.getIndex());
    }

    /**
     * 获取今日砸金猪免费次数
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_GOLDEN_PIG_FREE_TIMES_REQUEST_VALUE)
    public void freeTimes(TtmyGoldenPig.GoldenPigFreeTimesRequest request, ServerUser user) {
        goldenPigManager.sendFreeTimesResponse(user);
    }

    /**
     * 获取玩家今日砸金猪的剩余限制次数
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_GOLDEN_PIG_HIT_LIMIT_REQUEST_VALUE)
    public void hitLimit(TtmyGoldenPig.GoldenPigHitLimitRequest request, ServerUser user) {
        goldenPigManager.sendHitLimitResponse(user);
    }
}

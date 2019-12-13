package com.maple.game.osee.controller.fishing;

import com.maple.AppRun;
import com.maple.common.login.manager.LoginManager;
import com.maple.common.login.util.wxentity.WxAccessToken;
import com.maple.database.data.mapper.UserMapper;
import com.maple.game.osee.dao.log.mapper.AppGameLogMapper;
import com.maple.game.osee.dao.log.mapper.AppRankLogMapper;
import com.maple.game.osee.dao.log.mapper.AppRewardLogMapper;
import com.maple.game.osee.dao.log.mapper.AppRewardRankMapper;
import com.maple.game.osee.manager.MessageManager;
import com.maple.game.osee.timer.DailyTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AppRun.class})
public class FishingFishingUtilTest {

    @Autowired
    UserMapper userMapper;

    @Autowired
    AppRankLogMapper rankLogMapper;

    @Autowired
    AppRewardLogMapper rewardLogMapper;

    @Autowired
    AppGameLogMapper gameLogMapper;

    @Autowired
    AppRewardRankMapper rewardRankMapper;

    @Autowired
    MessageManager messageManager;

    @Autowired
    DailyTask dailyTask;

    @Autowired
    LoginManager loginManager;
    
    @Test
    public void test() {

        WxAccessToken wxAccessToken = loginManager.getWxAccessToken("071LEAmi2pWqXA0t1pmi2j6Pmi2LEAmb");
    }


}
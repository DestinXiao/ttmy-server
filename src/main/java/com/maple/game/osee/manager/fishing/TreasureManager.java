package com.maple.game.osee.manager.fishing;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.fishing.OseeFishingMessage.FishingGetTreasureResponse;
import com.maple.network.manager.NetManager;

/**
 * 1688捕鱼宝藏管理类
 */
@Component
public class TreasureManager {

	/**
	 * 宝藏费用
	 */
	public static final int[] treasureFee = { 200, 16000, 80000, 200000, 340000, 980000 };

	/**
	 * 宝藏奖励
	 */
	public static final int[][][] treasureReward = {

			// 水手宝藏
			{ { ItemId.MONEY.getId(), 100 }, { ItemId.MONEY.getId(), 200 }, { ItemId.MONEY.getId(), 300 },
					{ ItemId.MONEY.getId(), 400 }, { ItemId.LOTTERY.getId(), 1 }, { ItemId.LOTTERY.getId(), 2 } },
			// 水手长宝藏
			{ { ItemId.MONEY.getId(), 12000 }, { ItemId.MONEY.getId(), 16000 }, { ItemId.MONEY.getId(), 20000 },
					{ ItemId.LOTTERY.getId(), 1 }, { ItemId.LOTTERY.getId(), 2 }, { ItemId.LOTTERY.getId(), 3 } },
			// 三副宝藏
			{ { ItemId.MONEY.getId(), 60000 }, { ItemId.MONEY.getId(), 80000 }, { ItemId.MONEY.getId(), 100000 },
					{ ItemId.LOTTERY.getId(), 16 }, { ItemId.LOTTERY.getId(), 18 }, { ItemId.LOTTERY.getId(), 20 } },
			// 二副宝藏
			{ { ItemId.MONEY.getId(), 160000 }, { ItemId.MONEY.getId(), 200000 }, { ItemId.MONEY.getId(), 240000 },
					{ ItemId.LOTTERY.getId(), 30 }, { ItemId.LOTTERY.getId(), 40 }, { ItemId.LOTTERY.getId(), 50 } },
			// 大副宝藏
			{ { ItemId.MONEY.getId(), 280000 }, { ItemId.MONEY.getId(), 360000 }, { ItemId.MONEY.getId(), 440000 },
					{ ItemId.LOTTERY.getId(), 80 }, { ItemId.LOTTERY.getId(), 100 }, { ItemId.LOTTERY.getId(), 120 } },
			// 船长宝藏
			{ { ItemId.MONEY.getId(), 800000 }, { ItemId.MONEY.getId(), 1000000 }, { ItemId.MONEY.getId(), 1200000 },
					{ ItemId.LOTTERY.getId(), 100 }, { ItemId.LOTTERY.getId(), 200 }, { ItemId.LOTTERY.getId(), 300 } },

	};

	/**
	 * 抽取宝藏
	 */
	public void drawTreasure(ServerUser user, int treasureIndex, int drawIndex) {
		long cost = treasureFee[treasureIndex - 1];
		if (PlayerManager.checkItem(user, ItemId.MONEY, cost)) {
			PlayerManager.addItem(user, ItemId.MONEY, -cost, ItemChangeReason.LOTTERY_PAY, true);

			// 随机奖励
			int rewardIndex = ThreadLocalRandom.current().nextInt(6);

			// 添加道具
			int[] reward = treasureReward[treasureIndex - 1][rewardIndex];
			PlayerManager.addItem(user, reward[0], reward[1], ItemChangeReason.LOTTERY_WIN, true);

			FishingGetTreasureResponse.Builder builder = FishingGetTreasureResponse.newBuilder();
			builder.setIndex(treasureIndex);
			builder.setDrawIndex(drawIndex);
			builder.setRewardIndex(rewardIndex + 1);
			NetManager.sendMessage(OseeMsgCode.S_C_OSEE_FISHING_GET_TREASURE_RESPONSE_VALUE, builder, user);
		} else {
			NetManager.sendHintMessageToClient("金币不足，无法获取该宝藏", user);
		}
	}

}

package com.maple.game.osee.manager.fruitlaba;

import java.util.List;

import org.springframework.stereotype.Component;

import com.maple.engine.data.ServerUser;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLaBaLeaveRoomResponse;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLaBaRewardPoolGoldNumResponse;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLaBaStartRunResponse;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.FruitLabaTaskInfoResponse;
import com.maple.game.osee.proto.fruit.OseeFruitMessage.PlayerEnterFruitLaBaRoomResponse;
import com.maple.network.manager.NetManager;

/**
 * 水果拉霸发送数据管理类
 * @author lzr
 *
 * 2018年12月27日
 */
@Component
public class FruitLaBaSendDataManager {
	
	/**
	 * 发送给进入水果拉霸房间的玩家，房间中的数据
	 */
	public void sendRoomInfo(int msgCode,PlayerEnterFruitLaBaRoomResponse.Builder builder,ServerUser user) {
		NetManager.sendMessage(msgCode, builder, user);
	}
	
	/**
	 * 发送任务奖励信息
	 */
	public void sendTaskInfo(int msgCode,FruitLabaTaskInfoResponse.Builder builder,ServerUser user) {
		NetManager.sendMessage(msgCode, builder, user);
	}
	
	/**
	 * 向玩家发送奖池数量
	 */
	public void sendFruitLaBaRewardPoolNum(int msgCode, List<ServerUser> users, long poolGoldNum) {
		if(users==null||users.size()==0) {
			return;
		}
		FruitLaBaRewardPoolGoldNumResponse.Builder builder=FruitLaBaRewardPoolGoldNumResponse.newBuilder();
		builder.setPoolGoldNum(poolGoldNum);
		for (ServerUser user : users) {
			NetManager.sendMessage(msgCode, builder.build(), user);
		}
	}
	
	/**
	 * 离开房间，给玩家离开的响应
	 */
	public void sendLeaveRoomResponse(int msgCode,ServerUser user) {
		NetManager.sendMessage(msgCode, FruitLaBaLeaveRoomResponse.newBuilder().build(), user);
	}
	
	/**
	 * 发送玩家旋转(下注)结果数据
	 */
	public void sendRotateResultData(int msgCode, FruitLaBaStartRunResponse.Builder builder, ServerUser user) {
		NetManager.sendMessage(msgCode, builder, user);
	}
	
}

package com.maple.game.osee.manager.lobby;

import java.util.LinkedList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.maple.engine.data.ServerUser;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.ChangeBankPasswordResponse;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.CheckBankPasswordResponse;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.SaveMoneyResponse;
import com.maple.network.manager.NetManager;

/**
 * 保险箱管理类
 */
@Component
public class BankManager {

	/**
	 * 检查保险箱密码
	 */
	private static boolean checkBankPassword(ServerUser user, String bankPassword) {
		return bankPassword.equals(PlayerManager.getPlayerEntity(user).getBankPassword());
	}

	/**
	 * 检查保险箱密码任务
	 */
	public static void checkBankPasswordTask(ServerUser user, String bankPassword) {
		CheckBankPasswordResponse.Builder builder = CheckBankPasswordResponse.newBuilder();
		builder.setPassword(bankPassword);
		builder.setSuccess(checkBankPassword(user, bankPassword));
		NetManager.sendMessage(OseeMsgCode.S_C_OSEE_CHECK_BANK_PASSWORD_RESPONSE_VALUE, builder, user);
	}

	/**
	 * 存取金币任务
	 */
	public static void saveMoneyTask(ServerUser user, String bankPassword, long money) {
		if (money > 0) { // 存入
			if (!PlayerManager.checkItem(user, ItemId.MONEY, money)) {
				NetManager.sendHintMessageToClient("携带金币不足，无法执行存入操作", user);
				return;
			}

			if (money < 50000) {
				NetManager.sendHintMessageToClient("存入失败，单次最低存放金额为5万金币", user);
				return;
			}
		} else if (money < 0) { // 取出
			if (!checkBankPassword(user, bankPassword)) {
				NetManager.sendHintMessageToClient("保险箱密码错误，请重新输入", user);
				return;
			}
			
			if (!PlayerManager.checkItem(user, ItemId.BANK_MONEY, -money)) {
				NetManager.sendHintMessageToClient("保险箱金币不足，无法执行取出操作", user);
				return;
			}
		} else {
			NetManager.sendHintMessageToClient("存取金币数量不能为0", user);
			return;
		}

		List<ItemData> itemDatas = new LinkedList<>();
		itemDatas.add(new ItemData(ItemId.MONEY.getId(), -money));
		itemDatas.add(new ItemData(ItemId.BANK_MONEY.getId(), money));
		ItemChangeReason reason = money > 0 ? ItemChangeReason.BANK_IN : ItemChangeReason.BANK_OUT;
		PlayerManager.addItems(user, itemDatas, reason, true);

		SaveMoneyResponse.Builder builder = SaveMoneyResponse.newBuilder();
		builder.setSuccess(true);
		NetManager.sendMessage(OseeMsgCode.S_C_OSEE_SAVE_MONEY_RESPONSE_VALUE, builder, user);
	}

	/**
	 * 修改保险箱密码任务
	 */
	public static void changeBankPasswordTask(ServerUser user, String oldPassword, String newPassword) {
		if (!checkBankPassword(user, oldPassword)) {
			NetManager.sendHintMessageToClient("保险箱密码错误，请重新输入", user);
			return;
		}

		PlayerManager.getPlayerEntity(user).setBankPassword(newPassword);
		PlayerManager.updateEntities.add(PlayerManager.getPlayerEntity(user));

		ChangeBankPasswordResponse.Builder builder = ChangeBankPasswordResponse.newBuilder();
		builder.setSuccess(true);
		NetManager.sendMessage(OseeMsgCode.S_C_OSEE_CHANGE_BANK_PASSWORD_RESPONSE_VALUE, builder, user);
	}

}

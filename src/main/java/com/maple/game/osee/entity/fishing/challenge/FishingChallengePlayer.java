package com.maple.game.osee.entity.fishing.challenge;

import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.fishing.FishingGamePlayer;
import com.maple.game.osee.manager.PlayerManager;

/**
 * 捕鱼挑战赛玩家
 *
 * @author Junlong
 */
public class FishingChallengePlayer extends FishingGamePlayer {

    @Override
    public long getMoney() {
        return PlayerManager.getPlayerEntity(getUser()).getDragonCrystal();
    }

    @Override
    public void addMoney(long count) {
        PlayerManager.addItem(getUser(), ItemId.DRAGON_CRYSTAL, count, ItemChangeReason.FISHING_RESULT, false);
    }
}

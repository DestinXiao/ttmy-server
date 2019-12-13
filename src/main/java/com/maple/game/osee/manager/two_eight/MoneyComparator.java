package com.maple.game.osee.manager.two_eight;

import com.maple.game.osee.entity.two_eight.TwoEightPlayer;

import java.util.Comparator;

public class MoneyComparator implements Comparator<TwoEightPlayer> {

    @Override
    public int compare(TwoEightPlayer o1, TwoEightPlayer o2) {

        if (o1.getEntity().getMoney()>o2.getEntity().getMoney()){
            return -1;
        }else if (o1.getEntity().getMoney()<o2.getEntity().getMoney()){
            return 1;
        }else {
            return 0;
        }
    }
}

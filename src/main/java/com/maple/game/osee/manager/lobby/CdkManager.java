package com.maple.game.osee.manager.lobby;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maple.engine.data.ServerUser;
import com.maple.engine.manager.GsonManager;
import com.maple.game.osee.dao.data.entity.OseeCdkEntity;
import com.maple.game.osee.dao.data.entity.OseeCdkTypeEntity;
import com.maple.game.osee.dao.data.mapper.OseeCdkMapper;
import com.maple.game.osee.dao.data.mapper.OseeCdkTypeMapper;
import com.maple.game.osee.dao.log.entity.OseeExpendLogEntity;
import com.maple.game.osee.dao.log.mapper.OseeExpendLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.OseePublicData.ItemDataProto;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.UseCdkResponse;
import com.maple.network.manager.NetManager;

import io.netty.util.internal.ThreadLocalRandom;

/**
 * CDK管理类
 */
@Component
public class CdkManager {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OseeCdkMapper cdkMapper;

    @Autowired
    private OseeCdkTypeMapper cdkTypeMapper;

    @Autowired
    private OseeExpendLogMapper expendLogMapper;

    /**
     * CDK字符集
     */
    private static String CDK_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * 获取随机cdk头
     */
    private static String getRandomHead() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            builder.append(CDK_CHARS.toCharArray()[ThreadLocalRandom.current().nextInt(CDK_CHARS.length())]);
        }
        return builder.toString();
    }

    /**
     * 创建CDK类型
     */
    public String createCdkType(String typeName) {
        String startWith = null;
        for (int i = 0; i < 1000; i++) {
            String temp = getRandomHead();
            if (cdkTypeMapper.getByStart(temp) == null) {
                startWith = temp;
                break;
            }
        }

        if (!StringUtils.isEmpty(startWith)) {
            OseeCdkTypeEntity entity = new OseeCdkTypeEntity();
            entity.setName(typeName);
            entity.setStartWith(startWith);
            cdkTypeMapper.save(entity);

            return null;
        }

        return "cdk类型创建失败";
    }

    /**
     * 获取CDK类型
     */
    public List<OseeCdkTypeEntity> getCdkTypes() {
        return cdkTypeMapper.getAllCdkType();
    }

    /**
     * 创建cdk
     */
    public boolean createCdk(long typeId, int count, String rewards) {
        OseeCdkTypeEntity cdkType = cdkTypeMapper.getById(typeId);
        if (cdkType == null) {
            return false;
        }

        for (int i = 0; i < count; i++) {
            OseeCdkEntity cdk = new OseeCdkEntity();
            cdk.setRewards(rewards);
            cdk.setTypeId(typeId);

            String cdkey = null;
            for (int t = 0; t < 1000; t++) {
                StringBuilder temp = new StringBuilder(cdkType.getStartWith());
                while (temp.length() < 16) {
                    temp.append(CDK_CHARS.toCharArray()[ThreadLocalRandom.current().nextInt(CDK_CHARS.length())]);
                }
                if (cdkMapper.getByCdk(temp.toString()) == null) {
                    cdkey = temp.toString();
                    break;
                }
            }

            if (cdkey != null) {
                cdk.setCdk(cdkey);
                cdkMapper.save(cdk);
            } else {
                logger.info("cdk生成失败");
            }
        }
        return true;
    }

    /**
     * 删除对应类型的所有cdk和该类型
     */
    public void deleteCdk(long cdkTypeId) {
        // 删除cdk类型
        cdkTypeMapper.delete(cdkTypeId);
        // 删除对应的所有cdk
        cdkMapper.deleteByTypeId(cdkTypeId);
    }

    /**
     * 使用cdk
     */
    public void useCdk(ServerUser user, String cdk) {
        OseeCdkEntity entity = cdkMapper.getByCdk(cdk);
        if (entity != null) {
            OseeCdkEntity usedEntity = cdkMapper.getByUsed(entity.getTypeId(), user.getId());
            if (usedEntity != null) {
                NetManager.sendHintMessageToClient("您已使用过相同类型的CDK，无法多次使用", user);
                return;
            }

            if (entity.getUserId() == 0) {
                entity.setUserId(user.getId());
                entity.setNickname(user.getNickname());
                cdkMapper.update(entity);

                UseCdkResponse.Builder builder = UseCdkResponse.newBuilder();

                OseeExpendLogEntity log = new OseeExpendLogEntity();
                log.setUserId(user.getId());
                log.setNickname(user.getNickname());
                log.setPayType(4);

                ItemData[] itemArray = GsonManager.gson.fromJson(entity.getRewards(), ItemData[].class);
                for (ItemData item : itemArray) {
                    PlayerManager.addItem(user, item.getItemId(), item.getCount(), ItemChangeReason.USE_CDK, true);
                    ItemDataProto.Builder itemBuilder = ItemDataProto.newBuilder();
                    itemBuilder.setItemId(item.getItemId()).setItemNum(item.getCount());
                    builder.addItemData(itemBuilder);

                    ItemId itemId = ItemId.getItemIdById(item.getItemId());
                    if (itemId == ItemId.MONEY) {
                        log.setMoney(log.getMoney() + item.getCount());
                    } else if (itemId == ItemId.LOTTERY) {
                        log.setLottery(log.getLottery() + item.getCount());
                    } else if (itemId == ItemId.DIAMOND) {
                        log.setDiamond(log.getLottery() + item.getCount());
                    }
                }
                expendLogMapper.save(log); // 保存支出日志

                NetManager.sendMessage(OseeMsgCode.S_C_OSEE_USE_CDK_RESPONSE_VALUE, builder, user);
            } else {
                NetManager.sendHintMessageToClient("该cdk已被其他玩家兑换", user);
            }
        } else {
            NetManager.sendHintMessageToClient("cdk不存在", user);
        }
    }

}

package com.maple.game.osee.manager;

import com.maple.engine.container.UserContainer;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.data.entity.MessageEntity;
import com.maple.game.osee.dao.data.mapper.MessageMapper;
import com.maple.game.osee.dao.log.mapper.AppRankLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage;
import com.maple.network.manager.NetManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 消息/邮件管理类
 *
 * @author Junlong
 */
@Component
public class MessageManager {

    /**
     * 每位玩家最多可以接收的消息条数
     */
    public static final int MAX_MESSAGE_NUM = 20;

    private final MessageMapper messageMapper;

    @Autowired
    private AppRankLogMapper rankLogMapper;

    @Autowired
    public MessageManager(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 获取玩家消息列表
     */
    public void getMessageList(ServerUser user) {
        List<MessageEntity> list = messageMapper.getByToId(user.getId());
        OseeLobbyMessage.MessageListResponse.Builder builder = OseeLobbyMessage.MessageListResponse.newBuilder();
        for (MessageEntity message : list) {
            OseePublicData.MessageInfoProto.Builder messageInfo = OseePublicData.MessageInfoProto.newBuilder();
            messageInfo.setId(message.getId());
            messageInfo.setTime(message.getCreateTime().getTime());
            messageInfo.setRead(message.getRead());
            messageInfo.setReceive(message.getReceive());
            messageInfo.setTitle(message.getTitle());
            // 列表就不发生消息内容，避免数据量大
//            messageInfo.setContent(message.getContent());
//            if (message.getItems() != null) {
//                // 附件信息
//                for (ItemData itemData : message.getItems()) {
//                    OseePublicData.ItemDataProto.Builder itemDataProto = OseePublicData.ItemDataProto.newBuilder();
//                    itemDataProto.setItemId(itemData.getItemId());
//                    itemDataProto.setItemNum(itemData.getCount());
//                    messageInfo.addItems(itemDataProto);
//                }
//            }
            builder.addMessageInfo(messageInfo);
        }
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_MESSAGE_LIST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 获取玩家所有的消息总数量
     */
    public int getTotalMessageCount(long userId) {
        return messageMapper.getCountByToId(userId);
    }

    /**
     * 获取玩家未读消息数量
     */
    public void sendUnreadMessageCount(ServerUser user) {
        int unreadCount = messageMapper.getUnreadCount(user.getId());
        OseeLobbyMessage.UnreadMessageCountResponse.Builder builder = OseeLobbyMessage.UnreadMessageCountResponse.newBuilder();
        builder.setCount(unreadCount);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_UNREAD_MESSAGE_COUNT_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送消息
     */
    public long sendMessage(MessageEntity message) {
        if (message == null) {
            return 0L;
        }
        messageMapper.save(message);
        if (message.getFromId() == -1 && message.getToId() == 0) {
            // 全服邮件就立即给所有在线玩家发送一份
            List<ServerUser> onlineUsers = UserContainer.getActiveServerUsers();
            for (ServerUser user : onlineUsers) {
                if (user != null && user.isOnline()) {
                    MessageEntity messageEntity = new MessageEntity();
                    messageEntity.setFromId(message.getId() * message.getFromId());
                    messageEntity.setToId(user.getId());
                    messageEntity.setTitle(message.getTitle());
                    messageEntity.setContent(message.getContent());
                    messageEntity.setItems(message.getItems());
                    messageMapper.save(messageEntity);
                    sendUnreadMessageCount(user);
                }
            }
        } else {
            ServerUser toUser = UserContainer.getUserById(message.getToId());
            if (toUser != null && toUser.isOnline()) { // 如果在线就发送新消息通知
                sendUnreadMessageCount(toUser);
            }
        }
        return message.getId();
    }

    /**
     * 读取消息
     */
    public void readMessage(long id, ServerUser user) {
        MessageEntity message = messageMapper.getById(id);
        if (message == null) {
            NetManager.sendErrorMessageToClient("消息已不存在！", user);
            return;
        }
        // 设为已读
        message.setRead(true);
        messageMapper.update(message);

        OseeLobbyMessage.ReadMessageResponse.Builder builder = OseeLobbyMessage.ReadMessageResponse.newBuilder();

        OseePublicData.MessageInfoProto.Builder messageInfo = OseePublicData.MessageInfoProto.newBuilder();
        messageInfo.setId(message.getId());
        messageInfo.setRead(message.getRead());
        messageInfo.setReceive(message.getReceive());
        messageInfo.setTime(message.getCreateTime().getTime());
        messageInfo.setContent(message.getContent());
        if (message.getItems() != null) {
            // 附件信息
            for (ItemData itemData : message.getItems()) {
                OseePublicData.ItemDataProto.Builder itemDataProto = OseePublicData.ItemDataProto.newBuilder();
                itemDataProto.setItemId(itemData.getItemId());
                itemDataProto.setItemNum(itemData.getCount());
                messageInfo.addItems(itemDataProto);
            }
        }

        builder.setMessage(messageInfo);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_READ_MESSAGE_RESPONSE_VALUE, builder, user);
    }

    /**
     * 领取并删除消息
     */
    public void receiveMessageItems(long id, ServerUser user) {
        MessageEntity message = messageMapper.getById(id);
        if (message == null) {
            NetManager.sendErrorMessageToClient("消息已不存在！", user);
            return;
        }
        ItemData[] items = message.getItems();
        if (items != null) { // 如果消息有附件就要领取
            for (ItemData itemData : items) {
                PlayerManager.addItem(user, itemData.getItemId(), itemData.getCount(), ItemChangeReason.GIVE_GIFT, true);
            }
            message.setReceive(true);
            rankLogMapper.updateByEamilId(id, new Date());
        }
        // 删除消息
        messageMapper.deleteLogical(message);
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_TTMY_RECEIVE_MESSAGE_ITEMS_RESPONSE_VALUE,
                OseeLobbyMessage.ReceiveMessageItemsResponse.newBuilder().setResult(true),
                user);
    }
}

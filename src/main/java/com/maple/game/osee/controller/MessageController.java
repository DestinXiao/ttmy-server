package com.maple.game.osee.controller;

import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.manager.MessageManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

/**
 * 消息/邮件模块控制类
 *
 * @author Junlong
 */
@AppController
public class MessageController {

    @Autowired
    private MessageManager messageManager;

    /**
     * 检查方法
     */
    public void checker(Method taskMethod, Message req, ServerUser user, Long exp) throws Exception {
        taskMethod.invoke(this, req, user);
    }

    /**
     * 获取玩家消息列表
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_MESSAGE_LIST_REQUEST_VALUE)
    public void getMessageList(OseeLobbyMessage.MessageListRequest request, ServerUser user) {
        messageManager.getMessageList(user);
    }

    /**
     * 获取玩家未读消息数量
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_UNREAD_MESSAGE_COUNT_REQUEST_VALUE)
    public void getUnreadMessageCount(OseeLobbyMessage.UnreadMessageCountRequest request, ServerUser user) {
        messageManager.sendUnreadMessageCount(user);
    }

    /**
     * 玩家读取了消息
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_READ_MESSAGE_REQUEST_VALUE)
    public void readMessage(OseeLobbyMessage.ReadMessageRequest request, ServerUser user) {
        messageManager.readMessage(request.getId(), user);
    }

    /**
     * 领取消息附件并删除消息
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_RECEIVE_MESSAGE_ITEMS_REQUEST_VALUE)
    public void receiveMessageItems(OseeLobbyMessage.ReceiveMessageItemsRequest request, ServerUser user) {
        messageManager.receiveMessageItems(request.getId(), user);
    }
}

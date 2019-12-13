package com.maple.game.osee.controller;

import com.google.protobuf.Message;
import com.maple.engine.anotation.AppController;
import com.maple.engine.anotation.AppHandler;
import com.maple.engine.data.ServerUser;
import com.maple.game.osee.dao.data.entity.AgentEntity;
import com.maple.game.osee.manager.AgentManager;
import com.maple.game.osee.proto.OseeMessage;
import com.maple.game.osee.proto.agent.TtmyAgentMessage;
import com.maple.network.manager.NetManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;

/**
 * 代理模块控制类
 *
 * @author Junlong
 */
@AppController
public class AgentController {

    @Autowired
    private AgentManager agentManager;

    /**
     * 检查方法
     */
    public void checker(Method taskMethod, Message req, ServerUser user, Long exp) throws Exception {
        taskMethod.invoke(this, req, user);
    }

    /**
     * 检查玩家是否为代理
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_AGENT_CHECK_REQUEST_VALUE)
    public void agentCheck(TtmyAgentMessage.AgentCheckRequest request, ServerUser user) {
        agentManager.agentCheck(request.getPlayerId(), user);
    }

    /**
     * 获取代理分享信息
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_AGENT_SHARE_INFO_REQUEST_VALUE)
    public void shareInfo(TtmyAgentMessage.AgentShareInfoRequest request, ServerUser user) {
        agentManager.agentShareInfo(request.getPlayerId(), user);
    }

    /**
     * 一级代理获取团队信息
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_AGENT_TEAM_INFO_REQUEST_VALUE)
    public void teamInfo(TtmyAgentMessage.AgentTeamInfoRequest request, ServerUser user) {
        agentManager.agentTeamInfo(request, user);
    }

    /**
     * 二级代理下的会员详情
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_AGENT_MEMBER_INFO_REQUEST_VALUE)
    public void memberInfo(TtmyAgentMessage.AgentMemberInfoRequest request, ServerUser user) {
        agentManager.agentMemberInfo(request, user);
    }

    /**
     * 获取代理佣金详情
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_AGENT_COMMISSION_REQUEST_VALUE)
    public void commission(TtmyAgentMessage.AgentCommissionRequest request, ServerUser user) {
        agentManager.agentCommission(request, user);
    }

    /**
     * 代理佣金兑换
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_AGENT_COMMISSION_EXCHANGE_REQUEST_VALUE)
    public void commissionExchange(TtmyAgentMessage.AgentCommissionExchangeRequest request, ServerUser user) {
        agentManager.agentCommissionExchange(request, user);
    }

    /**
     * 代理佣金兑换物品
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_TTMY_AGENT_ITEM_EXCHANGE_REQUEST_VALUE)
    public void itemExchange(TtmyAgentMessage.AgentItemExchangeRequest request, ServerUser user) {
//        agentManager.agentItemExchange(request, user);
    }

    /**
     * 代理信息请求
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_AGENT_INFO_REQUEST_VALUE)
    public void agentInfo(TtmyAgentMessage.AgentInfoRequest request, ServerUser user) {
        agentManager.sendAgentInfoResponse(request.getYear(), user);
    }

    /**
     * 代理绑定状态
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_AGENT_STATE_REQUEST_VALUE)
    public void agentState(TtmyAgentMessage.AgentStateRequest request, ServerUser user) {
        AgentEntity agentPlayer = agentManager.getAgentInfoByPlayerId(request.getPlayerId());
        if (agentPlayer != null && agentPlayer.getAgentPlayerId() != null) {
            if (agentPlayer.getAgentPlayerId().equals(user.getId())) {
                NetManager.sendHintMessageToClient("该玩家已是你旗下成员", user);
            } else {
                NetManager.sendHintMessageToClient("该玩家已绑定其他代理", user);
            }
        } else {
            NetManager.sendHintMessageToClient("该玩家未绑定代理", user);
        }
    }

    /**
     * 玩家全民推广信息
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_PLAYER_AGENT_INFO_REQUEST_VALUE)
    public void playerAgentInfo(TtmyAgentMessage.PlayerAgentInfoRequest request, ServerUser user) {
        agentManager.sendPlayerAgentInfoResponse(user);
    }

    /**
     * 领取推广奖励
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_PLAYER_AGENT_RECEIVE_REQUEST_VALUE)
    public void playerAgentReceive(TtmyAgentMessage.PlayerAgentReceiveRequest request, ServerUser user) {
        agentManager.receiveActive(user);
    }

    /**
     * 领取推广奖励记录
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_PLAYER_AGENT_ACTIVE_EXCHANGE_REQUEST_VALUE)
    public void playerAgentActiveExchange(TtmyAgentMessage.PlayerAgentActiveExchangeRequest request, ServerUser user) {
        agentManager.agentActiveExchangeLog(request, user);
    }

    /**
     * 玩家绑定状态
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_PLAYER_BIND_STATE_REQUEST_VALUE)
    public void playerBindState(TtmyAgentMessage.PlayerBindStateRequest request, ServerUser user) {
        AgentEntity agentPlayer = agentManager.getAgentInfoByPlayerId(user.getId());
        TtmyAgentMessage.PlayerBindStateResponse.Builder builder = TtmyAgentMessage.PlayerBindStateResponse.newBuilder();
        builder.setState(agentPlayer != null && (agentPlayer.getAgentLevel() < 3 || agentPlayer.getAgentPlayerId() != null));
        NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_PLAYER_BIND_STATE_RESPONSE_VALUE, builder, user);
    }

    /**
     * 玩家绑定信息
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_PLAYER_BIND_INFO_REQUEST_VALUE)
    public void playerBindInfo(TtmyAgentMessage.PlayerBindInfoRequest request, ServerUser user) {
        AgentEntity agentPlayer = agentManager.getAgentInfoByPlayerId(user.getId());
        if (agentPlayer != null && (agentPlayer.getAgentPlayerId() != null || agentPlayer.getAgentLevel() < 3)) {
            NetManager.sendHintMessageToClient("您已绑定，不能再次绑定", user);
        } else {
            AgentEntity agent = agentManager.getAgentInfoByPlayerId(request.getPlayerId());
            TtmyAgentMessage.PlayerBindInfoResponse.Builder builder = TtmyAgentMessage.PlayerBindInfoResponse.newBuilder();
            builder.setEnable(true);
            builder.setPlayerId(agent.getPlayerId());
            builder.setNickname(agent.getPlayerName());
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_PLAYER_BIND_INFO_RESPONSE_VALUE, builder, user);
        }
    }

    /**
     * 玩家绑定
     */
    @AppHandler(msgCode = OseeMessage.OseeMsgCode.C_S_PLAYER_BIND_REQUEST_VALUE)
    public void playerBind(TtmyAgentMessage.PlayerBindRequest request, ServerUser user) {
        AgentEntity agentPlayer = agentManager.getAgentInfoByPlayerId(user.getId());
        if (agentPlayer != null && (agentPlayer.getAgentPlayerId() != null || agentPlayer.getAgentLevel() < 3)) {
            NetManager.sendHintMessageToClient("您已绑定，不能再次绑定", user);
        } else {
            AgentEntity agent = agentManager.getAgentInfoByPlayerId(request.getPlayerId());
            agentManager.bindPlayer(user.getEntity(), agent);
            TtmyAgentMessage.PlayerBindResponse.Builder builder = TtmyAgentMessage.PlayerBindResponse.newBuilder();
            builder.setSuccess(true);
            builder.setPlayerId(agent.getPlayerId());
            builder.setNickname(agent.getPlayerName());
            NetManager.sendMessage(OseeMessage.OseeMsgCode.S_C_PLAYER_BIND_RESPONSE_VALUE, builder, user);
        }
    }
}

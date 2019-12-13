package com.maple.game.osee.manager.lobby;

import com.google.gson.Gson;
import com.maple.database.config.redis.RedisHelper;
import com.maple.database.data.entity.UserAuthenticationEntity;
import com.maple.database.data.entity.UserEntity;
import com.maple.database.data.mapper.UserAuthenticationMapper;
import com.maple.database.data.mapper.UserMapper;
import com.maple.engine.config.AliSmsConfig;
import com.maple.engine.container.UserContainer;
import com.maple.engine.data.ServerUser;
import com.maple.engine.manager.GsonManager;
import com.maple.engine.utils.DateUtils;
import com.maple.engine.utils.SmsUtils;
import com.maple.engine.utils.ThreadPoolUtils;
import com.maple.game.osee.dao.data.entity.AgentEntity;
import com.maple.game.osee.dao.data.entity.MessageEntity;
import com.maple.game.osee.dao.data.entity.OseeNoticeEntity;
import com.maple.game.osee.dao.data.mapper.AgentMapper;
import com.maple.game.osee.dao.data.mapper.MessageMapper;
import com.maple.game.osee.dao.data.mapper.OseeNoticeMapper;
import com.maple.game.osee.dao.log.entity.GiveGiftLogEntity;
import com.maple.game.osee.dao.log.entity.OseeExpendLogEntity;
import com.maple.game.osee.dao.log.mapper.GiveGiftLogMapper;
import com.maple.game.osee.dao.log.mapper.OseeExpendLogMapper;
import com.maple.game.osee.dao.log.mapper.OseeRechargeLogMapper;
import com.maple.game.osee.entity.ItemChangeReason;
import com.maple.game.osee.entity.ItemData;
import com.maple.game.osee.entity.ItemId;
import com.maple.game.osee.entity.lobby.FunctionEnum;
import com.maple.game.osee.entity.lobby.PhoneCheck;
import com.maple.game.osee.entity.lobby.WechatShare;
import com.maple.game.osee.manager.AgentManager;
import com.maple.game.osee.manager.MessageManager;
import com.maple.game.osee.manager.PlayerManager;
import com.maple.game.osee.proto.OseeMessage.OseeMsgCode;
import com.maple.game.osee.proto.OseePublicData;
import com.maple.game.osee.proto.OseePublicData.ItemDataProto;
import com.maple.game.osee.proto.lobby.OseeLobbyMessage.*;
import com.maple.game.osee.util.ValidateUtil;
import com.maple.network.manager.NetManager;
import com.maple.network.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 大厅基本功能管理类
 */
@Component
public class CommonLobbyManager {

    private static Logger logger = LoggerFactory.getLogger(CommonLobbyManager.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OseeNoticeMapper noticeMapper;

    @Autowired
    private UserAuthenticationMapper authenticationMapper;

    @Autowired
    private OseeExpendLogMapper expendLogMapper;

    @Autowired
    private AliSmsConfig aliSmsConfig;

    @Autowired
    private GiveGiftLogMapper giftLogMapper;

    @Autowired
    private MessageManager messageManager;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private OseeRechargeLogMapper rechargeLogMapper;

    /**
     * VIP每日登录的奖励
     */
    private ItemData[][] VIP_REWARD_ITEMS = {
            {new ItemData(ItemId.SKILL_LOCK.getId(), 5), new ItemData(ItemId.SKILL_FROZEN.getId(), 5)}, // vip1
            {new ItemData(ItemId.SKILL_LOCK.getId(), 10), new ItemData(ItemId.SKILL_FROZEN.getId(), 5),
                    new ItemData(ItemId.SKILL_FAST.getId(), 2)}, // vip2
            {new ItemData(ItemId.SKILL_LOCK.getId(), 15), new ItemData(ItemId.SKILL_FROZEN.getId(), 5),
                    new ItemData(ItemId.SKILL_FAST.getId(), 5), new ItemData(ItemId.SKILL_CRIT.getId(), 2)}, // vip3
            {new ItemData(ItemId.SKILL_LOCK.getId(), 20), new ItemData(ItemId.SKILL_FROZEN.getId(), 5),
                    new ItemData(ItemId.SKILL_FAST.getId(), 10), new ItemData(ItemId.SKILL_CRIT.getId(), 2)}, // vip4
            {new ItemData(ItemId.SKILL_LOCK.getId(), 25), new ItemData(ItemId.SKILL_FROZEN.getId(), 5),
                    new ItemData(ItemId.SKILL_FAST.getId(), 10), new ItemData(ItemId.SKILL_CRIT.getId(), 2)}, // vip5
            {new ItemData(ItemId.SKILL_LOCK.getId(), 30), new ItemData(ItemId.SKILL_FROZEN.getId(), 10),
                    new ItemData(ItemId.SKILL_FAST.getId(), 15), new ItemData(ItemId.SKILL_CRIT.getId(), 5)}, // vip6
            {new ItemData(ItemId.SKILL_LOCK.getId(), 35), new ItemData(ItemId.SKILL_FROZEN.getId(), 15),
                    new ItemData(ItemId.SKILL_FAST.getId(), 15), new ItemData(ItemId.SKILL_CRIT.getId(), 5)}, // vip7
            {new ItemData(ItemId.SKILL_LOCK.getId(), 40), new ItemData(ItemId.SKILL_FROZEN.getId(), 15),
                    new ItemData(ItemId.SKILL_FAST.getId(), 20), new ItemData(ItemId.SKILL_CRIT.getId(), 5)}, // vip8
            {new ItemData(ItemId.SKILL_LOCK.getId(), 50), new ItemData(ItemId.SKILL_FROZEN.getId(), 20),
                    new ItemData(ItemId.SKILL_FAST.getId(), 30), new ItemData(ItemId.SKILL_CRIT.getId(), 10)}, // vip9
    };

    /**
     * 客服微信key
     */
    private final String SERVICE_WECHAT_KEY = "Server:Support:Wechat";

    /**
     * 客服二维码
     */
    private final String SERVICE_QRODE_KEY = "Server:Support:QRCode";

    /**
     * 首充记录Redis保存键命名空间
     */
    public static final String FIRST_CHARGE_KEY_NAMESPACE = "Server:FirstCharge:%d";

    /**
     * 月卡每日奖励赠送状态
     */
    public static final String MONTH_CARD_DAILY_REWARDS_KEY_NAMESPACE = "Server:MonthCard:DailyRewards:%d";

    /**
     * vip等级每日登录奖励
     */
    public static final String VIP_DAILY_REWARDS_KEY_NAMESPACE = "Server:Vip:DailyRewards:%d";

    /**
     * vip金币的补足记录key
     */
    public static final String VIP_MONEY_ENOUGH_KEY_NAMESPACE = "Server:Vip:MoneyEnough:%d";

    /**
     * 公告列表
     */
    private List<OseeNoticeEntity> notices = new LinkedList<>();

    /**
     * 实名认证验证表
     */
    private Map<Long, PhoneCheck> phoneCheckMap = new HashMap<>();

    /**
     * 重置密码验证表
     */
    private Map<String, PhoneCheck> resetPasswordCheckMap = new HashMap<>();

    /**
     * 手机验证cd
     */
    private final int coolDown = 300000;

    @Autowired
    private AgentManager agentManager;

    public CommonLobbyManager() {
        ThreadPoolUtils.TASK_SERVICE_POOL.schedule(this::refreshNotice, 5, TimeUnit.SECONDS);
    }

    /**
     * 刷新公告列表
     */
    public void refreshNotice() {
        notices = noticeMapper.getAll();
        for (int i = 1; i <= notices.size(); i++) {
            if (notices.get(i - 1).getIndex() != i) {
                notices.get(i - 1).setIndex(i);
                noticeMapper.update(notices.get(i - 1));
            }
        }
    }

    /**
     * 交换公告顺序
     */
    public boolean changeNotice(long id, int type) {
        int index = -1;
        for (int i = 0; i < notices.size(); i++) {
            if (notices.get(i).getId() == id) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return false;
        }
        if (index + type < 0 || index + type >= notices.size()) {
            return false;
        }

        notices.get(index + type).setIndex(index);
        notices.get(index).setIndex(index + type);
        noticeMapper.update(notices.get(index + type));
        noticeMapper.update(notices.get(index));

        refreshNotice();

        return true;
    }

    /**
     * 用户实名认证手机验证
     */
    public void authenticatePhoneCheck(ServerUser user, String phone) {
        long nowTime = System.currentTimeMillis();
        PhoneCheck checkLog = phoneCheckMap.get(user.getId());
        if (checkLog != null && nowTime - checkLog.getCheckTime().getTime() < coolDown) {
            NetManager.sendHintMessageToClient("请勿频繁获取验证码", user);
            return;
        }
        int checkCode = ThreadLocalRandom.current().nextInt(100000, 1000000);

        if (SmsUtils.sendAliyunSms(phone, "SMS_158050408", "{\"code\":\"" + checkCode + "\"}", aliSmsConfig)) {
            PhoneCheck phoneCheck = new PhoneCheck();
            phoneCheck.setCheckCode(checkCode);
            phoneCheck.setCheckTime(new Date());
            phoneCheck.setPhoneNum(phone);

            phoneCheckMap.put(user.getId(), phoneCheck);
            AuthenticatePhoneCheckResponse.Builder builder = AuthenticatePhoneCheckResponse.newBuilder();
            builder.setResult(true);
            NetManager.sendMessage(OseeMsgCode.S_C_OSEE_AUTHENTICATE_PHONE_CHECK_RESPONSE_VALUE, builder, user);
        } else {
            NetManager.sendHintMessageToClient("验证短信发送失败", user);
        }
    }

    /**
     * 用户重置密码手机验证
     */
    public void resetPasswordPhoneCheck(ServerUser user, String username) {
        try {
            String phone = userMapper.findByUsername(username).getPhonenum();
            if (!StringUtils.isEmpty(phone)) {
                long nowTime = System.currentTimeMillis();
                PhoneCheck checkLog = resetPasswordCheckMap.get(username);
                if (checkLog != null && nowTime - checkLog.getCheckTime().getTime() < coolDown) {
                    NetManager.sendHintMessageToClient("请勿频繁获取验证码", user);
                    return;
                }
                int checkCode = ThreadLocalRandom.current().nextInt(100000, 1000000);

                if (SmsUtils.sendAliyunSms(phone, "SMS_158051452", "{\"code\":\"" + checkCode + "\"}", aliSmsConfig)) {
                    PhoneCheck phoneCheck = new PhoneCheck();
                    phoneCheck.setCheckCode(checkCode);
                    phoneCheck.setCheckTime(new Date());
                    phoneCheck.setPhoneNum(phone);

                    resetPasswordCheckMap.put(username, phoneCheck);
                    ResetPasswordPhoneCheckResponse.Builder builder = ResetPasswordPhoneCheckResponse.newBuilder();
                    builder.setResult(true);
                    NetManager.sendMessage(OseeMsgCode.S_C_OSEE_RESET_PASSWORD_PHONE_CHECK_RESPONSE_VALUE, builder, user);
                } else {
                    NetManager.sendHintMessageToClient("验证短信发送失败", user);
                }
            } else {
                NetManager.sendHintMessageToClient("该账号未绑定手机号，请联系客服微信:" + getSupportWechat(), user);
            }
        } catch (Exception e) {
            NetManager.sendHintMessageToClient("该账号不存在", user);
        }
    }

    /**
     * 用户实名认证
     */
    public void userAuthenticate(ServerUser user, String realName, String idCardNum, String phoneNum, int checkCode) {
//        PhoneCheck phoneCheck = phoneCheckMap.get(user.getId());
//        if (phoneCheck == null || !phoneCheck.getPhoneNum().equals(phoneNum)) {
//            NetManager.sendHintMessageToClient("请先进行手机验证", user);
//            return;
//        }
//
//        if (phoneCheck.getCheckCode() != checkCode) {
//            NetManager.sendHintMessageToClient("验证码不正确", user);
//            return;
//        }

        UserAuthenticationEntity entity = authenticationMapper.getByUserId(user.getId());
        if (entity != null) {
            NetManager.sendHintMessageToClient("请勿重复认证", user);
            return;
        }

        entity = authenticationMapper.getByIdcardNo(idCardNum);
        if (entity != null) {
            NetManager.sendHintMessageToClient("该身份证已被绑定", user);
            return;
        }

        entity = authenticationMapper.getByPhoneNo(phoneNum);
        if (entity != null) {
            NetManager.sendHintMessageToClient("该手机已被绑定", user);
            return;
        }

        if (checkIdCardNum(realName, idCardNum)) {
            phoneCheckMap.remove(user.getId());

            entity = new UserAuthenticationEntity();
            entity.setIdcardNo(idCardNum);
            entity.setName(realName);
            entity.setPhoneNo(phoneNum);
            entity.setUserId(user.getId());
            authenticationMapper.save(entity);

            sendAuthentication(user);

            List<ItemData> itemDatas = new LinkedList<>();
            itemDatas.add(new ItemData(ItemId.MONEY.getId(), 10000));
//            itemDatas.add(new ItemData(ItemId.LOTTERY.getId(), 30));
            PlayerManager.addItems(user, itemDatas, ItemChangeReason.AUTHENTICATION, true);

            OseeExpendLogEntity log = new OseeExpendLogEntity();
            log.setUserId(user.getId());
            log.setNickname(user.getNickname());
            log.setPayType(1);
            log.setMoney(50000);
//            log.setLottery(30);
            expendLogMapper.save(log);

            SubmitAuthenticateResponse.Builder builder = SubmitAuthenticateResponse.newBuilder();
            builder.addRewards(ItemDataProto.newBuilder().setItemId(ItemId.MONEY.getId()).setItemNum(10000)); // 1w金币
//            builder.addRewards(ItemDataProto.newBuilder().setItemId(ItemId.LOTTERY.getId()).setItemNum(30)); // 30奖券
            NetManager.sendMessage(OseeMsgCode.S_C_OSEE_SUBMIT_AUTHENTICATE_RESPONSE_VALUE, builder, user);
        } else {
            NetManager.sendHintMessageToClient("绑定失败，请输入正确的身份证信息", user);
        }
    }

    /**
     * 重置密码
     */
    public void resetPassword(ServerUser user, String username, String password, int checkCode) {
        try {
            String phone = userMapper.findByUsername(username).getPhonenum();
            if (!StringUtils.isEmpty(phone)) {
                PhoneCheck phoneCheck = resetPasswordCheckMap.get(username);
                if (phoneCheck == null || !phoneCheck.getPhoneNum().equals(phone)) {
                    NetManager.sendHintMessageToClient("请先进行手机验证", user);
                    return;
                }

                if (phoneCheck.getCheckCode() != checkCode) {
                    NetManager.sendHintMessageToClient("验证码不正确", user);
                    return;
                }

                ServerUser loginUser = UserContainer.getUserByUsername(username);
                loginUser.getEntity().setPassword(password);
                userMapper.update(loginUser.getEntity());

                NetManager.sendHintMessageToClient("密码修改成功", user);
            } else {
                NetManager.sendHintMessageToClient("该账号未绑定手机号，请联系客服微信:" + getSupportWechat(), user);
            }
        } catch (Exception e) {
            NetManager.sendHintMessageToClient("该账号不存在", user);
        }
    }

    /**
     * 微信分享
     */
    public void wechatShare(ServerUser user) {
        WechatShare share = new Gson().fromJson(RedisHelper.get("Wechat:Share:" + user.getId()), WechatShare.class);
        if (share == null || !DateUtils.isSameDay(new Date(), share.getShareDate())) {
            RedisHelper.set("Wechat:Share:" + user.getId(), GsonManager.gson.toJson(new WechatShare(new Date())));
            WechatShareResponse.Builder builder = WechatShareResponse.newBuilder();
            builder.setRewardMoney(3000);
            PlayerManager.addItem(user, ItemId.MONEY, 3000, ItemChangeReason.WECHAT_SHARE, true);
            NetManager.sendMessage(OseeMsgCode.S_C_OSEE_WECHAT_SHARE_RESPONSE_VALUE, builder, user);
        } else {
            NetManager.sendHintMessageToClient("今日分享奖励已领取", user);
        }
    }

    /**
     * 检查身份证号码
     */
    @SuppressWarnings("unchecked")
    private boolean checkIdCardNum(String realName, String idCardNum) {
        String host = "https://idenauthen.market.alicloudapi.com";
        String path = "/idenAuthentication";
        String appcode = "b8701ad8a9bf49f095756bbbc9eee914";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "APPCODE " + appcode);
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> bodys = new HashMap<>();
        bodys.put("idNo", idCardNum);
        bodys.put("name", realName);

        try {
            String response = HttpUtil.doPost(host + path, bodys, headers);
            Map<String, Object> resultMap = GsonManager.gson.fromJson(response, Map.class);
            if (Integer.parseInt((String) resultMap.get("respCode")) == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 发送重置密码手机号消息
     */
    public void sendGetResetPasswordPhoneNumResponse(ServerUser user, String username) {
        UserEntity entity = userMapper.findByUsername(username);
        if (entity != null) {
            GetResetPasswordPhoneNumResponse.Builder builder = GetResetPasswordPhoneNumResponse.newBuilder();
            builder.setUsername(username);
            String phoneNo = entity.getPhonenum();
            char[] phoneNoArray = phoneNo.toCharArray();
            for (int i = 3; i < 7; i++) {
                phoneNoArray[i] = '*';
            }
            phoneNo = new String(phoneNoArray);
            builder.setPhoneNum(phoneNo);
            int msgCode = OseeMsgCode.S_C_OSEE_GET_RESET_PASSWORD_PHONE_NUM_RESPONSE_VALUE;
            NetManager.sendMessage(msgCode, builder, user);
        } else {
            NetManager.sendHintMessageToClient("该手机号未绑定账号，请联系客服微信:" + getSupportWechat(), user);
        }
    }

    /**
     * 发送客服微信消息
     */
    public void sendServiceWechatResponse(ServerUser user) {
        ServiceWechatResponse.Builder builder = ServiceWechatResponse.newBuilder();
        builder.setWechat(getSupportWechat());
        builder.setQrcode(getSupportQRCode());
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_SERVICE_WECHAT_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送公告消息
     */
    public void sendNoticeListResponse(ServerUser user) {
        NoticeListResponse.Builder builder = NoticeListResponse.newBuilder();
        for (OseeNoticeEntity notice : notices) {
            long nowTime = System.currentTimeMillis();
            if (notice.getStartTime().getTime() < nowTime && notice.getEndTime().getTime() > nowTime) {
                NoticeProto.Builder proto = NoticeProto.newBuilder();
                proto.setTitle(notice.getTitle());
                proto.setContent(notice.getContent());
                builder.addNotice(proto);
            }
        }
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_NOTICE_LIST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送认证消息
     */
    public void sendAuthentication(ServerUser user) {
        UserAuthenticationEntity entity = authenticationMapper.getByUserId(user.getId());
        AuthenticateInfoResponse.Builder builder = AuthenticateInfoResponse.newBuilder();
        if (entity != null) {
            builder.setIdCardNum(entity.getIdcardNo());
            builder.setPhoneNum(entity.getPhoneNo());
            builder.setRealName(entity.getName());
        }
        NetManager.sendMessage(OseeMsgCode.S_C_OSEE_AUTHENTICATE_INFO_RESPONSE_VALUE, builder, user);
    }

    /**
     * 获取客服微信
     */
    public String getSupportWechat() {
        String wechat = RedisHelper.get(SERVICE_WECHAT_KEY);
        return StringUtils.isEmpty(wechat) ? "" : wechat;
    }

    /**
     * 设置客服微信
     */
    public void setSupportWechat(String wechat) {
        RedisHelper.set(SERVICE_WECHAT_KEY, wechat);
    }

    /**
     * 获取客服二维码
     */
    public String getSupportQRCode() {
        String QRCode = RedisHelper.get(SERVICE_QRODE_KEY);
        return StringUtils.isEmpty(QRCode) ? "" : QRCode;
    }

    /**
     * 设置客服二维码
     */
    public void setSupportQRCode(String QRCode) {
        RedisHelper.set(SERVICE_QRODE_KEY, QRCode);
    }

    /**
     * 用户设置账号手机号验证
     */
    public void accountPhoneCheck(String phoneNum, ServerUser user) {
        if (!ValidateUtil.isPhoneNumber(phoneNum)) {
            NetManager.sendErrorMessageToClient("输入的手机号码格式错误！", user);
            return;
        }
        UserEntity userEntity = userMapper.findByUsername(phoneNum);
        if (userEntity != null) {
            NetManager.sendHintMessageToClient("该手机号已被使用！请更换！", user);
            return;
        }
        long nowTime = System.currentTimeMillis();
        PhoneCheck checkLog = phoneCheckMap.get(user.getId());
        // 冷却时间
        long coolDown = 60 * 1000;
        if (checkLog != null && nowTime - checkLog.getCheckTime().getTime() < coolDown) {
            NetManager.sendHintMessageToClient("请勿频繁获取验证码", user);
            return;
        }
        int checkCode = ThreadLocalRandom.current().nextInt(100000, 1000000);
        // 发送短信
        if (SmsUtils.sendAliyunSms(phoneNum, "SMS_151475518", "{\"code\":\"" + checkCode + "\"}", aliSmsConfig)) {
            PhoneCheck phoneCheck = new PhoneCheck();
            phoneCheck.setCheckCode(checkCode);
            phoneCheck.setCheckTime(new Date());
            phoneCheck.setPhoneNum(phoneNum);
            // 放入内存储存
            phoneCheckMap.put(user.getId(), phoneCheck);
            AccountPhoneCheckResponse.Builder builder = AccountPhoneCheckResponse.newBuilder();
            builder.setResult(true);
            NetManager.sendMessage(OseeMsgCode.S_C_TTMY_ACCOUNT_PHONE_CHECK_RESPONSE_VALUE, builder, user);
        } else {
            NetManager.sendHintMessageToClient("验证短信发送失败", user);
        }
    }

    /**
     * 玩家设置账号
     */
    public void accountSet(AccountSetRequest request, ServerUser user) {
        String phoneNum = request.getPhoneNum();
        int checkCode = request.getCheckCode();
        // 传输的已经加密的内容
        String password = request.getPassword();

        if (!ValidateUtil.isPhoneNumber(phoneNum)) {
            NetManager.sendErrorMessageToClient("输入的手机号码格式错误！", user);
            return;
        }
        // 判断验证码是否正确
        PhoneCheck phoneCheck = phoneCheckMap.get(user.getId());
        if (phoneCheck == null || !phoneCheck.getPhoneNum().equals(phoneNum)) {
            NetManager.sendHintMessageToClient("请先进行手机验证", user);
            return;
        }
        if (phoneCheck.getCheckCode() != checkCode) {
            NetManager.sendHintMessageToClient("验证码不正确", user);
            return;
        }
        if (userMapper.findByUsername(phoneNum) != null) {
            NetManager.sendHintMessageToClient("该手机号已被使用！请更换！", user);
            return;
        }
        UserEntity userEntity = user.getEntity();
        userEntity.setUsername(phoneNum);
        userEntity.setPassword(password);
        // 账号即为手机号
        userEntity.setPhonenum(phoneNum);
        // 更新玩家信息
        userMapper.update(userEntity);
        // 奖励金币一万
        PlayerManager.addItem(user, ItemId.MONEY, 10000, ItemChangeReason.ACCOUNT_SET, true);

        AccountSetResponse.Builder builder = AccountSetResponse.newBuilder();
        builder.addRewards(ItemDataProto.newBuilder().setItemId(ItemId.MONEY.getId()).setItemNum(10000)); // 奖励金币一万
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_ACCOUNT_SET_RESPONSE_VALUE, builder, user);
    }

    /**
     * 确认赠送礼物
     */
    public void confirmGiveGift(ConfirmGiveGiftRequest request, ServerUser user) {
        long toPlayerId = request.getToPlayerId();


        ServerUser toUser = UserContainer.getUserById(toPlayerId);
        if (toUser == null) {
            NetManager.sendErrorMessageToClient("赠送失败：目标ID玩家不存在！", user);
            return;
        }

        AgentEntity agentEntity = agentManager.getAgentInfoByPlayerId(user.getId());
        AgentEntity toPlayerAgentEntity = agentManager.getAgentInfoByPlayerId(toPlayerId);
        int agentLevel = agentEntity.getAgentLevel();
        int toPlayerAgentLevel = toPlayerAgentEntity.getAgentLevel();
        Long agentPlayerId = agentEntity.getAgentPlayerId();



        AgentEntity agentInfoByPlayerId = agentManager.getAgentInfoByPlayerId(agentPlayerId);

//        logger.info((agentLevel == 3 && toPlayerAgentLevel == 1) + " ");
        if (agentLevel == 3 && toPlayerAgentLevel == 1) {
            NetManager.sendErrorMessageToClient("赠送失败：需要加入联盟才可以赠送！", user);
            return;
        }

        // 玩家未绑定二级代理不能赠送礼物
        if (agentLevel == 3 && agentEntity.getAgentPlayerId() == null || agentLevel == 3 && agentManager.getAgentInfoByPlayerId(agentEntity.getAgentPlayerId()).getAgentLevel() != 2) {
            NetManager.sendErrorMessageToClient("赠送失败：需要加入联盟才可以赠送！", user);
            return;
        }

        if ((agentLevel == 1 && toPlayerAgentLevel == 3) || (agentLevel == 1 && !toPlayerAgentEntity.getAgentPlayerId().equals(agentEntity.getPlayerId()))) {
            NetManager.sendErrorMessageToClient("赠送失败：与该玩家未在同一个联盟！", user);
            return;
        }
        Long agentPlayerId1 = toPlayerAgentEntity.getAgentPlayerId();
        Long playerId = agentEntity.getPlayerId();

        if ((agentLevel == 2 && toPlayerAgentLevel == 2) || (agentLevel == 2 && !agentEntity.getAgentPlayerId().equals(toPlayerId))) {
            NetManager.sendErrorMessageToClient("赠送失败：与该玩家未在同一个联盟！", user);
            return;
        }

        if (!agentEntity.getAgentPlayerId().equals(toPlayerAgentEntity.getAgentPlayerId())) {
            NetManager.sendErrorMessageToClient("赠送失败：与该玩家未在同一个联盟！", user);
            return;
        }


            if (PlayerManager.getPlayerVipLevel(user) < 2) {
                NetManager.sendErrorMessageToClient("赠送失败：需要VIP2及以上才开放赠送！", user);
                return;
            }

            if (toPlayerId == user.getId()) {
                NetManager.sendErrorMessageToClient("赠送失败：不能自己赠送给自己礼物！", user);
                return;
            }

            ConfirmGiveGiftResponse.Builder builder = ConfirmGiveGiftResponse.newBuilder();
            builder.setToPlayerId(toUser.getId());
            builder.setToPlayerName(toUser.getNickname());
            NetManager.sendMessage(OseeMsgCode.S_C_TTMY_CONFIRM_GIVE_GIFT_RESPONSE_VALUE, builder, user);

    }

    /**
     * 赠送礼物
     */
    public void giveGift(GiveGiftRequest request, ServerUser user) {
        long toPlayerId = request.getToPlayerId();
        int itemId = request.getItemId();
        long itemCount = request.getItemCount();

        ServerUser toUser = UserContainer.getUserById(toPlayerId);
        if (toUser == null) {
            NetManager.sendErrorMessageToClient("赠送失败：目标ID玩家不存在！", user);
            return;
        }




     if (itemCount <= 0) {
            NetManager.sendErrorMessageToClient("赠送失败：赠送数量必须大于0个！", user);
            return;
        }

        ItemId item = ItemId.getItemIdById(itemId);
        if (item == null) {
            NetManager.sendErrorMessageToClient("赠送失败：赠送物品ID有误！", user);
            return;
        }

        // 检查赠送玩家自己携带的物品是否足够赠送
        if (!PlayerManager.checkItem(user, itemId, itemCount)) {
            NetManager.sendErrorMessageToClient("赠送失败：要赠送的物品数量不足！", user);
            return;
        }

        // 检查对方是否还可以接收赠送消息
        if (messageManager.getTotalMessageCount(toPlayerId) >= MessageManager.MAX_MESSAGE_NUM) {
            NetManager.sendErrorMessageToClient("赠送失败：被赠人消息已满！", user);
            return;
        }
        // 赠送的物品名称
        String itemName = item.getInfo();

        // 扣除赠送玩家赠送出去的物品
        PlayerManager.addItem(user, itemId, -itemCount, ItemChangeReason.GIVE_GIFT, true);

        // 发送消息给被赠送玩家
        MessageEntity message = new MessageEntity();
        message.setFromId(user.getId());
        message.setToId(toPlayerId);
        message.setTitle("礼物赠送");
        message.setContent(String.format(
                "收到玩家 %s(ID:%d) 赠送的 %s*%d",
                user.getNickname(), user.getId(), itemName, itemCount
        ));
        message.setItems(new ItemData[]{
                new ItemData(itemId, itemCount)
        });
        messageManager.sendMessage(message);

        // 保存赠送记录
        GiveGiftLogEntity giftLogEntity = new GiveGiftLogEntity();
        giftLogEntity.setFromId(user.getId());
        giftLogEntity.setFromName(user.getNickname());
        giftLogEntity.setToId(toPlayerId);
        giftLogEntity.setToName(toUser.getNickname());
        giftLogEntity.setGiftName(itemName);
        giftLogEntity.setGiftNum(itemCount);
        giftLogMapper.save(giftLogEntity);

        // 发送赠送成功响应
        GiveGiftResponse.Builder builder = GiveGiftResponse.newBuilder();
        builder.setFromPlayerId(user.getId());
        builder.setToPlayerId(toPlayerId);
        builder.setItemId(itemId);
        builder.setItemCount(itemCount);
        builder.setTime(giftLogEntity.getCreateTime().getTime());
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_GIVE_GIFT_RESPONSE_VALUE, builder, user);
    }

    /**
     * 更改用户昵称
     */
    public void changeNickname(ServerUser user, String nickname) {
        if (StringUtils.isEmpty(nickname)) {
            NetManager.sendErrorMessageToClient("昵称不能为空！", user);
            return;
        }
        if (nickname.length() > 20) {
            NetManager.sendErrorMessageToClient("昵称长度不能超过20！", user);
            return;
        }
        if (user.getNickname().equals(nickname)) {
            NetManager.sendErrorMessageToClient("昵称未改变！", user);
            return;
        }

        String timesKey = "Server:ChangeNickname:" + user.getId();
        // 获取已修改昵称次数
        String timesValue = RedisHelper.get(timesKey);
        int times = 0;
        if (!StringUtils.isEmpty(timesValue)) {
            times = Integer.parseInt(timesValue);
        }
        if (times > 0) { // 已经改过名称
            int cost = 10;
            // 检查改名需要花费的钻石是否足够
            if (!PlayerManager.checkItem(user, ItemId.DIAMOND, cost)) {
                NetManager.sendErrorMessageToClient("钻石不足！", user);
                return;
            }
            // 扣除钻石
            PlayerManager.addItem(user, ItemId.DIAMOND, -cost, ItemChangeReason.CHANGE_NICKNAME, true);
        }
        UserEntity entity = user.getEntity();
        entity.setNickname(nickname);
        // 更改昵称
        userMapper.updateNickname(entity);
        times++;
        RedisHelper.set(timesKey, String.valueOf(times));

        ChangeNicknameResponse.Builder builder = ChangeNicknameResponse.newBuilder();
        builder.setNickname(nickname);
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_CHANGE_NICKNAME_RESPONSE_VALUE, builder, user);
    }

    /**
     * 赠送玩家月卡每日赠送奖励
     */
    public void sendDailyMonthCardRewards(ServerUser user) {
        if (PlayerManager.getItemNum(user, ItemId.MONTH_CARD) <= 0) { // 月卡到期了或者没开通月卡
            return;
        }
        String key = String.format(MONTH_CARD_DAILY_REWARDS_KEY_NAMESPACE, user.getId());
        String value = RedisHelper.get(key);
        boolean give = true;
        if (!StringUtils.isEmpty(value)) {
            // 判断今天是否已经赠送了每日奖励
            give = !LocalDate.parse(value).isEqual(LocalDate.now());
        }
        if (give) {
            MessageEntity message = new MessageEntity();
            message.setTitle("月卡每日奖励");
            message.setContent("");
            message.setToId(user.getId());
            // 每日奖励：锁定卡10张、10张冰冻卡、5张急速卡、2张暴击卡
            message.setItems(new ItemData[]{
                    new ItemData(ItemId.SKILL_LOCK.getId(), 10),
                    new ItemData(ItemId.SKILL_FROZEN.getId(), 10),
                    new ItemData(ItemId.SKILL_FAST.getId(), 5),
                    new ItemData(ItemId.SKILL_CRIT.getId(), 2),
            });
            messageManager.sendMessage(message);
            // 今日领取信息存入数据库
            RedisHelper.set(key, LocalDate.now().toString());
        }
    }

    /**
     * 赠送每日VIP登录奖励
     */
    public void sendDailyVipRewards(ServerUser user) {
        int vipLevel = PlayerManager.getPlayerVipLevel(user);
        if (vipLevel < 1) { // 不是VIP
            return;
        }
        String key = String.format(VIP_DAILY_REWARDS_KEY_NAMESPACE, user.getId());
        String value = RedisHelper.get(key);
        boolean give = true;
        if (!StringUtils.isEmpty(value)) {
            // 判断今天是否已经赠送了每日奖励
            give = !LocalDate.parse(value).isEqual(LocalDate.now());
        }
        if (give) {
            MessageEntity message = new MessageEntity();
            message.setTitle("VIP每日奖励");
            message.setContent("");
            message.setToId(user.getId());
            // 每日奖励物品
            // 获取对应vip等级的奖励
            ItemData[] vipRewardItem = VIP_REWARD_ITEMS[vipLevel - 1];
            message.setItems(vipRewardItem);
            messageManager.sendMessage(message);
            // 今日领取信息存入数据库
            RedisHelper.set(key, LocalDate.now().toString());
        }
    }

    /**
     * 检查VIP7及以上的金币数量，每日上线金币不足指定数量，自动补足
     */
    public void checkVipMoneyEnough(ServerUser user) {
        int vipLevel = PlayerManager.getPlayerVipLevel(user);
        if (vipLevel < 7 || vipLevel > 9) {
            return;
        }
        String key = String.format(VIP_MONEY_ENOUGH_KEY_NAMESPACE, user.getId());
        String value = RedisHelper.get(key);
        boolean give = true;
        if (!StringUtils.isEmpty(value)) {
            // 判断今天是否已经补足过金币
            give = !LocalDate.parse(value).isEqual(LocalDate.now());
        }
        if (give) {
            long[] moneyLimit = {5000000, 20000000, 80000000};
            // 跟金币阈值的差值
            long limit = PlayerManager.getItemNum(user, ItemId.MONEY) - moneyLimit[vipLevel - 7];
            if (limit < 0) { // 金币不够就补足
                PlayerManager.addItem(user, ItemId.MONEY.getId(), -limit, null, true);
                // 补足信息存入数据库
                RedisHelper.set(key, LocalDate.now().toString());
            }
        }
    }

    /**
     * 获取玩家今日的剩余充值限额
     */
    public void moneyLimitRest(ServerUser user) {
        // 获取玩家今日充值的金额数量 数据库中存的是分为单位
        long todayRecharge = rechargeLogMapper.getTodayRecharge(user.getId()) / 100;
        RechargeLimitRestResponse.Builder builder = RechargeLimitRestResponse.newBuilder();
        builder.setLimitRest(20000 - todayRecharge); // 每日充值剩余限额 2w的上限
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_RECHARGE_LIMIT_REST_RESPONSE_VALUE, builder, user);
    }

    /**
     * 获取各功能模块启用状态
     */
    public void functionState(ServerUser user) {
        FunctionStateResponse.Builder builder = FunctionStateResponse.newBuilder();
        builder.addFunctionState(OseePublicData.FunctionStateProto.newBuilder().
                setFuncId(FunctionEnum.BUY_SKILL.getId()).setState(true)); // 1-商城购买技能功能
        // 判断6元首充是否有效
        boolean firstCharge = false;
        String key = String.format(FIRST_CHARGE_KEY_NAMESPACE, user.getId());
        String value = RedisHelper.get(key);
        if (!StringUtils.isEmpty(value)) { // 已经首充了
            firstCharge = true;
        }
        builder.addFunctionState(OseePublicData.FunctionStateProto.newBuilder().
                setFuncId(FunctionEnum.FIRST_CHARGE.getId()).setState(!firstCharge)); // 2-首充功能

        boolean chessCards = false;
        AgentEntity agentEntity = agentMapper.getByPlayerId(user.getId());
        if (agentEntity != null && (agentEntity.getAgentLevel() != 3 || agentEntity.getAgentPlayerId() != null)) { // 有代理信息
            long agentId = 0;
            if (agentEntity.getAgentLevel() == 3) { // 会员
                AgentEntity secondAgent = agentMapper.getByPlayerId(agentEntity.getAgentPlayerId());
                agentId = secondAgent.getAgentPlayerId();
            } else if (agentEntity.getAgentLevel() == 2) { // 二级代理
                agentId = agentEntity.getAgentPlayerId();
            } else if (agentEntity.getAgentLevel() == 1) { // 一级代理
                agentId = agentEntity.getPlayerId();
            }
            String openChessCardsStr = RedisHelper.get("Agent:OpenChessCards:" + agentId);
            if (!StringUtils.isEmpty(openChessCardsStr)) {
                long openChessCards = Long.parseLong(openChessCardsStr);
                if (openChessCards == 1) { // 开启了棋牌模块
                    chessCards = true;
                }
            }
        }
        builder.addFunctionState(OseePublicData.FunctionStateProto.newBuilder()
                .setFuncId(FunctionEnum.CHESS_CARDS.getId())
                .setState(chessCards) // 3-棋牌功能
                .build());
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_FUNCTION_STATE_RESPONSE_VALUE, builder, user);
    }

    /**
     * 检测顽疾是否需要救济金并给予救济金
     */
    public static void checkReliefMoney(ServerUser user) {
        long playerMoney = PlayerManager.getPlayerMoney(user);
        if (playerMoney < 3000) {
            String key = String.format("Server:ReliefMoney:%d", user.getId());
            String value = RedisHelper.get(key);
            int restTimes; // 剩余次数
            if (!StringUtils.isEmpty(value)) {
                String[] split = value.split(",");
                LocalDate date = LocalDate.parse(split[0]);
                if (!date.isEqual(LocalDate.now())) { // 不是当天的，重置为新的领取
                    restTimes = 3;
                } else {
                    restTimes = Integer.parseInt(split[1]);
                }
            } else {
                // 领取新的救济
                restTimes = 3;
            }
            if (restTimes <= 0) { // 今日领取次数已用完
                return;
            }
            restTimes--;
            // 更新记录数据
            RedisHelper.set(key, LocalDate.now().toString() + "," + restTimes);
            int reliefMoney = 3000;
            // vip领取的金币翻等级+1倍
            int vipLevel = PlayerManager.getPlayerVipLevel(user);
            if (vipLevel > 0) {
                reliefMoney *= vipLevel + 1;
            }
            // 增加金币
            PlayerManager.addItem(user, ItemId.MONEY, reliefMoney, null, true);
            GetReliefMoneyResponse.Builder builder = GetReliefMoneyResponse.newBuilder();
            builder.setMoney(reliefMoney);
            builder.setRestTimes(restTimes);
            NetManager.sendMessage(OseeMsgCode.S_C_TTMY_GET_RELIEF_MONEY_RESPONSE_VALUE, builder, user);
            logger.info("玩家[{}]领取救济金[{}],剩余次数[{}]", user.getNickname(), reliefMoney, restTimes);
        }
    }

    /**
     * 获取玩家的赠送记录
     */
    public void giveGiftLog(ServerUser user, int pageNo, int pageSize) {
        GiveGiftLogResponse.Builder builder = GiveGiftLogResponse.newBuilder();
        builder.setPageNo(pageNo);
        StringBuilder query = new StringBuilder(" where 1 = 1");
        query.append(" and from_id = ").append(user.getId());
        // 获取数据总条数
        long count = giftLogMapper.getCount(query.toString());
        builder.setTotalCount(count);
        // 按照时间降序
        query.append(" order by create_time desc");
        // 分页数据
        query.append(" limit ").append((pageNo - 1) * pageSize).append(",").append(pageSize);
        List<GiveGiftLogEntity> entityList = giftLogMapper.getList(query.toString());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        for (GiveGiftLogEntity entity : entityList) {
            GiveGiftLogProto.Builder log = GiveGiftLogProto.newBuilder();
            log.setToId(entity.getToId());
            log.setToName(entity.getToName());
            log.setGiftName(entity.getGiftName());
            log.setGiftNum(entity.getGiftNum());
            log.setTime(dateFormat.format(entity.getCreateTime()));
            builder.addLogs(log);
        }
        NetManager.sendMessage(OseeMsgCode.S_C_TTMY_GIVE_GIFT_LOG_RESPONSE_VALUE, builder, user);
    }

    /**
     * 发送后台的系统全服邮件给玩家
     */
    public void sendSystemMail(ServerUser user) {
        long userId = user.getId();
        // 检测全服邮件是否发送给了该玩家，如果没有就要发送消息给玩家
        // tips:定义的系统邮件发送给玩家的fromId=系统邮件自身ID*自身的fromId(为-1)
        List<MessageEntity> messageList = messageMapper.getServerMessageList();
        for (MessageEntity message : messageList) {
            long fromId = message.getId() * message.getFromId();
            // 该玩家没有收到过系统邮件且邮件创建时间在玩家注册时间之后
            if (message.getCreateTime().getTime() >= PlayerManager.getPlayerEntity(user).getCreateTime().getTime()
                    && messageMapper.getFromCountByToId(fromId, userId) <= 0) {
                MessageEntity entity = new MessageEntity();
                entity.setFromId(fromId);
                entity.setToId(userId);
                entity.setTitle(message.getTitle());
                entity.setContent(message.getContent());
                entity.setItems(message.getItems());
                messageManager.sendMessage(entity);
            }
        }
    }
}

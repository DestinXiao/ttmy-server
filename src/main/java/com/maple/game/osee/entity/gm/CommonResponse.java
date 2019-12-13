package com.maple.game.osee.entity.gm;

import java.io.Serializable;
import java.util.HashMap;

/**
 * GM通用返回数据封装
 */
public class CommonResponse implements Serializable {

    private static final long serialVersionUID = -1388182003031343566L;

    private Boolean success;

    private String errCode;

    private String errMsg;

    private Object data;

    public CommonResponse(boolean success, String errCode, String errMsg,
                          Object data) {
        this.success = success;
        this.errCode = errCode;
        this.errMsg = errMsg;
        this.data = data;
    }

    public CommonResponse(String errCode, String errMsg) {
        this.success = false;
        this.errCode = errCode;
        this.errMsg = errMsg;
        this.data = new HashMap<>();
    }

    public CommonResponse(boolean success) {
        this.success = success;
        this.errCode = null;
        this.errMsg = null;
        this.data = new HashMap<>();
    }

    public CommonResponse(Object data) {
        this.success = true;
        this.errCode = null;
        this.errMsg = null;
        this.data = data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

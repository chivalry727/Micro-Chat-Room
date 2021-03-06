package com.micro.im.req;

import lombok.Data;

/**
 * @author Mr.zxb
 * @date 2020-06-10 20:10:35
 */
@Data
public class UserRegisterReq {
    private String account;
    private String nickname;
    private String email;
    private String verifyCode;
    private String password;
}

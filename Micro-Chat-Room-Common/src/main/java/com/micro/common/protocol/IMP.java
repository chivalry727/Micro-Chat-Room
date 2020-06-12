package com.micro.common.protocol;

/**
 * 自定义IM即时通信协议， Instant Messaging Protocol
 *
 * @author Mr.zxb
 * @date 2020-05-25
 **/
public enum IMP {

    /**
     * 系统消息
     */
    SYSTEM("system"),
    /**
     * 登陆指令
     */
    LOGIN("login"),
    /**
     * 登出指令
     */
    LOGOUT("logout"),
    /**
     * 聊天信息
     */
    CHAT("chat"),
    /**
     * 送鲜花
     */
    FLOWER("flower"),

    /**
     * 私聊消息类型
     */
    FRIEND("friend"),

    /**
     * 群聊消息类型
     */
    GROUP("group");

    private String name;

    IMP(String name) {
        this.name = name;
    }

    public static boolean isIMP(String protocol) {
        return protocol.matches("^\\[(SYSTEM|LOGIN|LOGOUT|CHAT)]");
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "IMP{" +
                "name='" + name + '\'' +
                '}';
    }
}

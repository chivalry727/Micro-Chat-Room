package com.micro.im.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import java.io.Serializable;
/**
 * <p>
 *  好友分组实体
 * </p>
 *
 * @author Mr.zxb
 * @since 2020-06-10
 */
public class UserFriendsGroup extends Model<UserFriendsGroup> {

    private static final long serialVersionUID = 1L;

    /**
     * 分组主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 好友分组名称
     */
    private String name;

    /**
     * 所属用户
     */
    private Long userId;

    private Integer type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    @Override
    protected Serializable pkVal() {
        return null;
    }

    @Override
    public String toString() {
        return "UserFriendsGroup{" +
        ", id=" + id +
        ", name=" + name +
        ", userId=" + userId +
        "}";
    }
}

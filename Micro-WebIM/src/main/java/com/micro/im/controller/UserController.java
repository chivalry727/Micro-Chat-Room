package com.micro.im.controller;

import com.google.common.collect.Lists;
import com.micro.common.constant.FileType;
import com.micro.common.dto.UserDTO;
import com.micro.common.response.ResultPageVO;
import com.micro.common.response.ResultVO;
import com.micro.common.util.TokenUtil;
import com.micro.im.configuration.RedisClient;
import com.micro.im.entity.User;
import com.micro.im.entity.UserFriendsGroup;
import com.micro.im.req.*;
import com.micro.im.resp.*;
import com.micro.im.service.FileService;
import com.micro.im.service.MessageService;
import com.micro.im.service.UserService;
import com.micro.im.vo.Mine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.micro.common.code.BusinessCode.*;
import static com.micro.common.constant.ServerConstant.OFFLINE;
import static com.micro.common.constant.ServerConstant.ONLINE;
import static com.micro.common.response.ResultVO.fail;
import static com.micro.common.response.ResultVO.success;

/**
 * 用户 ctrl
 *
 * @author Mr.zxb
 * @date 2020-06-10 09:22
 */
@RestController
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private FileService fileService;

    @Autowired
    private MessageService messageService;

    /**
     * 获取用户和好友列表信息
     *
     * @param token
     * @return
     */
    @GetMapping("/getList.do")
    public ResultVO<GetListResp> getList(@RequestParam String token) {
        log.info("获取用户列表 req: {}", token);
        if (StringUtils.isBlank(token)) {
            return fail(PARAM_ERROR);
        }
        UserDTO userDto = getUserDto(token);
        if (userDto == null) {
            return fail(NO_LOGIN);
        }
        Long id = userDto.getId();
        GetListResp resp = userService.getList(id);
        return success(resp);
    }

    /**
     * 获取群员列表
     *
     * @param id
     * @return
     */
    @GetMapping("/getMembers.do")
    public ResultVO<GetMembersResp> getMembers(@RequestParam Long id) {
        log.info("获取群员列表，群ID: {}", id);
        GetMembersResp members = userService.getMembers(id);
        return success(members);
    }

    /**
     * 发送邮件验证码
     *
     * @param req
     * @return
     */
    @PostMapping("/sendVerifyCode.do")
    public ResultVO sendVerifyCode(@RequestBody VerifyCodeReq req) {
        log.info("发送邮件验证码：{}", req);
        String code = TokenUtil.randomCode("0123456789", 6);
        String message = "注册帐号验证码：" + code;
        boolean result;
        if (StringUtils.isNoneBlank(req.getEmail())) {
            result = messageService.sendEmailMessage(req.getEmail(), message);
            if (!result) {
                return fail(SEND_VERIFY_CODE_FAILED);
            }
            // 120s 过期
            redisClient.set(req.getEmail(), code, 120);
            return success();
        } else {
            return fail(EMAIL_REQUIRED);
        }
    }

    /**
     * 注册用户
     *
     * @param userRegisterReq
     * @return
     */
    @PostMapping("/register.do")
    public ResultVO register(@RequestBody UserRegisterReq userRegisterReq) {
        log.info("注册新用户: {}", userRegisterReq.getNickname());
        String redisCode = (String) redisClient.get(userRegisterReq.getEmail());
        if (!Objects.equals(userRegisterReq.getVerifyCode(), redisCode)) {
            return fail(VERIFY_CODE_INVALID);
        }
        userService.register(userRegisterReq);
        return success();
    }

    /**
     * 账号是否已存在
     *
     * @param account
     * @return
     */
    @GetMapping("/accountExists.do")
    public ResultVO accountExists(@RequestParam String account) {
        log.info("查看账户是否存在：{}", account);
        boolean exists = userService.accountExists(account);
        return success(exists);
    }

    /**
     * 用户登录
     *
     * @param req
     * @return
     */
    @PostMapping("/login.do")
    public ResultVO login(@RequestBody UserLoginReq req) {
        log.info("用户登录: {}", req.getAccount());
        User user = userService.login(req.getAccount(), req.getPassword());
        if (user != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setAccount(user.getAccount());
            userDTO.setNickname(user.getNickname());
            userDTO.setAge(user.getAge());
            userDTO.setAvatar(user.getAvatarAddress());
            userDTO.setSign(user.getSign());
            userDTO.setAddress(user.getArea());

            // 生成token
            String token = TokenUtil.getToken();
            // 缓存用户到Redis
            redisClient.set(token, userDTO, 3600);
            redisClient.set(user.getId().toString(), ONLINE);
            return success(token);
        }
        return fail(USER_INVALID);
    }

    /**
     * 登出
     *
     * @return
     */
    @GetMapping("/logout.do")
    public ResultVO logout(HttpServletRequest request) {
        String token = request.getHeader("token");
        log.info("用户退出登录：{}", token);
        if (StringUtils.isNotBlank(token)) {
            redisClient.remove(token);
            Long userId = getUserDto(token).getId();
            redisClient.set(userId.toString(), OFFLINE);
        }
        return success();
    }

    /**
     * 上传图片
     *
     * @param file
     * @return
     */
    @PostMapping("/uploadImg.do")
    public ResultVO<UploadImageResp> uploadImg(@RequestParam("file") MultipartFile file) {
        log.info("上传图片：{}", file);
        String filepath = fileService.uploadFile(file, FileType.IMG).getSrc();
        return success(new UploadImageResp(filepath));
    }

    /**
     * 上传文件
     *
     * @param file
     * @return
     */
    @PostMapping("/uploadFile.do")
    public ResultVO<UploadFileResp> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("上传文件：{}", file);
        UploadFileResp fileResp = fileService.uploadFile(file, FileType.FILE);
        return success(fileResp);
    }

    /**
     * 修改在线状态
     *
     * @param request
     * @param status
     * @return
     */
    @GetMapping("/modifyStatus.do")
    public ResultVO modifyStatus(HttpServletRequest request, @RequestParam String status) throws Exception {
        UserDTO dto = getUserDto(request);
        if (dto != null) {
            log.info("修改用户在线状态：{}:{}", dto.getNickname(), status);
            redisClient.set(dto.getId().toString(), status);
            userService.sendUserStatusMessage(dto.getId(), Objects.equals(status, ONLINE) ? status : OFFLINE);
            return success();
        }
        return fail(PARAM_ERROR);
    }

    /**
     * 修改签名
     *
     * @param request
     * @param req
     * @return
     */
    @PostMapping("/modifySign.do")
    public ResultVO modifySign(HttpServletRequest request, @RequestBody ModifySignReq req) {
        UserDTO dto = getUserDto(request);
        if (dto != null) {
            log.info("更改用户签名：{}:{}", dto.getNickname(), req);
            User user = new User();
            user.setId(dto.getId());
            user.setSign(req.getSign());
            userService.updateUser(user);
            return success();
        }
        return fail(NO_LOGIN);
    }

    private UserDTO getUserDto(HttpServletRequest request) {
        return getUserDto(request.getHeader("token"));
    }

    private UserDTO getUserDto(String token) {
        if (token == null) {
            return null;
        }
        return (UserDTO) redisClient.get(token);
    }

    /**
     * 获取好友推荐
     *
     * @param token
     * @return
     */
    @GetMapping("/getRecommend.do")
    public ResultVO<List<RecommendResp>> getRecommend(@RequestParam String token) {
        log.info("获取好友推荐：{}", token);
        UserDTO userDto = getUserDto(token);
        if (userDto != null) {
            List<User> users = userService.getRecommend(userDto.getId());
            return getResultVO(users);
        }
        return success(Lists.newArrayList());
    }

    /**
     * 根据昵称或账号查找好友总数
     *
     * @return
     */
    @GetMapping("/findFriendTotal.do")
    public ResultVO<FindFriendTotalResp> findFriendTotal(@RequestParam String value) {
        log.info("根据账号或昵称查找好友总数: {}", value);
        List<User> userByAccount = userService.findUserByAccountAndName(value, null);
        return success(new FindFriendTotalResp(userByAccount.size()));
    }

    /**
     * 根据昵称或是账号查找好友列表
     *
     * @return
     */
    @GetMapping("/findFriend.do")
    public ResultVO findFriend(@RequestParam String value, @RequestParam Integer page) {
        log.info("根据账号或昵称查找好友: {}:{}", value, page);
        List<User> users = userService.findUserByAccountAndName(value, page);
        return getResultVO(users);
    }

    private ResultVO getResultVO(List<User> users) {
        List<RecommendResp> recommendResp = users.stream().map(user -> {
            RecommendResp resp = new RecommendResp();
            resp.setId(user.getId().toString());
            resp.setNickname(user.getNickname());
            resp.setAvatar(user.getAvatarAddress());
            resp.setSign(Optional.ofNullable(user.getSign()).orElse(""));
            return resp;
        }).collect(Collectors.toList());
        return success(recommendResp);
    }

    /**
     * 发送添加好友申请请求
     *
     * @param
     */
    @PostMapping("/sendAddFriendReq.do")
    public ResultVO sendAddFriendReq(@RequestBody AddFriendReq req) throws Exception {
        log.info("发送添加好友请求req：{}", req);
        userService.sendAddFriendReq(req);
        return success();
    }

    /**
     * 确认添加好友申请
     *
     * @param
     */
    @PostMapping("/confirmAddFriend.do")
    public ResultVO confirmAddFriend(@RequestBody ConfirmAddFriendReq req) throws Exception {
        log.info("确认添加好友申请req：{}", req);
        userService.confirmAddFriend(req);
        return success();
    }

    /**
     * 拒绝好友添加
     *
     * @return
     */
    @PostMapping("/refuseFriend.do")
    public ResultVO refuseFriend(RefuseFriendReq req) throws Exception {
        log.info("拒绝好友申请req：{}", req);
        userService.refuseFriend(req);
        return success();
    }

    /**
     * 添加群聊
     *
     * @param
     */
    @PostMapping("/addGroup.do")
    public ResultVO addGroup() {
        return success();
    }

    /**
     * 获取消息盒子信息
     *
     * @param
     */
    @PostMapping("/getMsgBox.do")
    public ResultPageVO getMsgBox(@RequestBody MsgBoxReq req) {
        log.info("获取消息盒子信息req: {}", req);
        List<MsgBoxResp> messageBox = userService.getMessageBox(req);
        Integer boxCount = userService.getMessageBoxCount(req.getUserId());
        return ResultPageVO.success(messageBox, boxCount, req.getUserId());
    }

    /**
     * 查看离线消息盒子数量
     *
     * @param uid
     * @return
     */
    @GetMapping("getMsgBoxCount.do")
    public ResultVO<Integer> getMsgBoxCount(@RequestParam Long uid) {
        Integer boxCount = userService.getMessageBoxCount(uid);
        return success(boxCount);
    }

    /**
     * 将未读消息设置为已读
     *
     * @param uid
     * @return
     */
    @GetMapping("setMessageRead.do")
    public ResultVO setMessageRead(@RequestParam Long uid) {
        log.info("将消息设为已读：{}", uid);
        userService.setMessageRead(uid);
        return success();
    }

    /**
     * 添加好友分组
     * @param req
     * @return
     */
    @PostMapping("addMyGroup.do")
    public ResultVO<UserFriendsGroup> addMyGroup(@RequestBody AddMyGroupReq req) {
        log.info("新增好友分组：{}", req);
        return success(userService.addMyGroup(req));
    }

    /**
     * 编辑分组名称
     * @return
     */
    @PostMapping("editGroupName.do")
    public ResultVO editGroupName(@RequestBody EditGroupNameReq req) {
        log.info("修改好友分组：{}", req);
        userService.editGroupName(req);
        return success();
    }

    /**
     * 删除分组
     * @return
     */
    @GetMapping("delMyGroup.do")
    public ResultVO<Long> delMyGroup(@RequestParam Long id, @RequestParam Long userId) {
        log.info("删除好友分组：{}", id);
        userService.deleteFriendGroup(id);
        return success(userService.getDefaultFriendGroup(userId));
    }

    @GetMapping("moveFriendGroup.do")
    public ResultVO<Mine> moveFriendGroup(@RequestParam Long userId,
                                          @RequestParam Long friendId,
                                          @RequestParam Long groupId,
                                          @RequestParam Long old) {
        userService.moveFriendGroup(userId, groupId, old);
        return success(userService.getMine(friendId));
    }

    @GetMapping("editFriendRemark.do")
    public ResultVO editFriendRemark(@RequestParam Long userId,
                                     @RequestParam String nickName,
                                     @RequestParam Long friendId) {
        userService.editFriendRemark(userId, friendId, nickName);
        return success();
    }

}

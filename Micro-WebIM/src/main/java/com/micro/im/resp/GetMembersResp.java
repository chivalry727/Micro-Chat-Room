package com.micro.im.resp;

import com.micro.im.vo.MembersVO;
import lombok.Data;

import java.util.List;

/**
 * @author Mr.zxb
 * @date 2020-06-09 21:00:34
 */
@Data
public class GetMembersResp {
    private List<MembersVO> list;
}

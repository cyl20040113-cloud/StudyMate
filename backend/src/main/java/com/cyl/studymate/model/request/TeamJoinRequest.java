package com.cyl.studymate.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户加入队伍请求体体
 */
@Data
public class TeamJoinRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * TeamId
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;
}

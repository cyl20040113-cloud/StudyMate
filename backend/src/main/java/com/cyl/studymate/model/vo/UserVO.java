package com.cyl.studymate.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户公开信息
 * 用于用户搜索、推荐、匹配、队伍创建人等场景
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 id
     */
    private long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 标签列表 json
     */
    private String tags;

    /**
     * 星球编号
     */
    private String planetCode;

    /**
     * 简介
     */
    private String profile;
}
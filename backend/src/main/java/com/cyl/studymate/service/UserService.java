package com.cyl.studymate.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyl.studymate.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cyl.studymate.model.request.UserUpdateRequest;
import com.cyl.studymate.model.vo.UserDetailVO;
import com.cyl.studymate.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 31893
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2026-08-07 15:53:39
 */
public interface UserService extends IService<User> {


    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);


    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList
     * @return
     */
    Page<User> searchUsersByTags(
            List<String> tagNameList,
            long pageNum,
            long pageSize
    );

    /**
     * 更新用户信息
     *
     * @param loginUser
     * @return
     */
    int updateUser(UserUpdateRequest userUpdateRequest, User loginUser);

    /**
     * 获取当前登录用户信息
     *
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户，未登录时返回 null。
     */
    User getLoginUserOrNull(HttpServletRequest request);

    /**
     * 转换为可对外返回的用户信息。
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户详细信息
     *
     * @param user
     * @return
     */
    UserDetailVO getUserDetailVO(User user);

    /**
     * 获取用户详细信息列表
     *
     * @param userList
     * @return
     */
    List<UserDetailVO> getUserDetailVOList(List<User> userList);

    List<UserVO> getUserVOList(List<User> userList);

    Page<UserVO> getUserVOPage(Page<User> userPage);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    boolean isAdmin(User loginUser);

    /**
     * 推荐匹配
     * @param num
     * @param loginUser
     * @return
     */
    List<User> matchUsers(long num, User loginUser);

    /**
     * 删除用户
     *
     * @param userId 用户 id
     * @param loginUser 当前登录用户
     * @return 是否删除成功
     */
    boolean deleteUser(long userId, User loginUser);


}

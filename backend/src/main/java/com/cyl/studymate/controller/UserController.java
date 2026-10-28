package com.cyl.studymate.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyl.studymate.common.BaseResponse;
import com.cyl.studymate.common.DeleteRequest;
import com.cyl.studymate.common.ErrorCode;
import com.cyl.studymate.common.ResultUtils;
import com.cyl.studymate.common.exception.BusinessException;
import com.cyl.studymate.model.domain.User;
import com.cyl.studymate.model.request.UserLoginRequest;
import com.cyl.studymate.model.request.UserRegisterRequest;
import com.cyl.studymate.model.request.UserUpdateRequest;
import com.cyl.studymate.model.vo.UserDetailVO;
import com.cyl.studymate.model.vo.UserVO;
import com.cyl.studymate.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户接口
 */

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
//            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<UserDetailVO> userLogin(
            @RequestBody UserLoginRequest userLoginRequest,
            HttpServletRequest request) {

        //1. 校验请求参数
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        //2. 用户登录
        User user = userService.userLogin(
                userAccount,
                userPassword,
                request
        );

        //3. 返回当前用户的详细信息
        return ResultUtils.success(
                userService.getUserDetailVO(user)
        );
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/current")
    public BaseResponse<UserDetailVO> getCurrentUser(
            HttpServletRequest request) {

        //1. 获取当前登录用户
        User currentUser = userService.getLoginUser(request);

        //2. 返回当前用户详细信息
        return ResultUtils.success(
                userService.getUserDetailVO(currentUser)
        );
    }

    @GetMapping("/search")
    public BaseResponse<List<UserDetailVO>> searchUsers(
            String username,
            HttpServletRequest request) {

        //1. 仅管理员可以查询
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        //2. 构造查询条件
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();

        if (StringUtils.isNotBlank(username)) {
            userQueryWrapper.like("username", username);
        }

        //3. 查询用户
        List<User> userList = userService.list(userQueryWrapper);

        //4. 管理员返回用户详细信息
        return ResultUtils.success(
                userService.getUserDetailVOList(userList)
        );
    }

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList 标签列表
     * @param pageNum     当前页
     * @param pageSize    每页大小
     * @param request
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<Page<UserVO>> searchUsersByTags(
            @RequestParam(required = false) List<String> tagNameList, long pageNum,
            long pageSize,
            HttpServletRequest request) {

        //1. 校验标签
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }

        //2. 校验登录状态
        userService.getLoginUser(request);

        //3. 根据标签分页查询
        Page<User> userPage = userService.searchUsersByTags(tagNameList, pageNum, pageSize);

        //4. 转成公开 UserVO
        Page<UserVO> userVOPage =
                userService.getUserVOPage(userPage);

        return ResultUtils.success(userVOPage);
    }

    @GetMapping("/recommend")
    public BaseResponse<Page<UserVO>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数不合法");
        }
        User loginUser = userService.getLoginUser(request);
        String redisKey = String.format("studymate:user:recommend:%s:%s:%s", loginUser.getId(), pageNum, pageSize);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        //如果有缓存，直接读取
        Page<UserVO> userVOPage = (Page<UserVO>) valueOperations.get(redisKey);
        if (userVOPage != null) {
            return ResultUtils.success(userVOPage);
        }
        //无缓存，查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        userVOPage = userService.getUserVOPage(userPage);
        //写缓存,30s过期
        try {
            valueOperations.set(redisKey, userVOPage, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return ResultUtils.success(userVOPage);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        //1. 校验参数是否为空
        if (userUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        //2. 校验权限
        //3. 触发更新
        int result = userService.updateUser(userUpdateRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {

        if (deleteRequest == null
                || deleteRequest.getId() == null
                || deleteRequest.getId() <= 0) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        User loginUser = userService.getLoginUser(request);

        boolean result =
                userService.deleteUser(
                        deleteRequest.getId(),
                        loginUser
                );

        return ResultUtils.success(result);
    }

    /**
     * 获取最匹配的用户
     *
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<UserVO>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getUserVOList(userService.matchUsers(num, loginUser)));
    }
}

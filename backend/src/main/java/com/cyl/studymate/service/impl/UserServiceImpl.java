package com.cyl.studymate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyl.studymate.common.ErrorCode;
import com.cyl.studymate.common.exception.BusinessException;
import com.cyl.studymate.mapper.TeamMapper;
import com.cyl.studymate.mapper.UserMapper;
import com.cyl.studymate.mapper.UserTeamMapper;
import com.cyl.studymate.model.domain.Team;
import com.cyl.studymate.model.domain.User;
import com.cyl.studymate.model.domain.UserTeam;
import com.cyl.studymate.model.request.UserUpdateRequest;
import com.cyl.studymate.model.vo.UserDetailVO;
import com.cyl.studymate.model.vo.UserVO;
import com.cyl.studymate.service.UserService;
import com.cyl.studymate.utils.BCryptUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.cyl.studymate.constant.UserConstant.ADMIN_ROLE;
import static com.cyl.studymate.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author 31893
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2026-08-07 15:53:39
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserTeamMapper userTeamMapper;

    @Resource
    private TeamMapper teamMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }
        //账户不能包含特殊字符
        final String validPattern = "^[a-zA-Z0-9_.]+$";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含非法字符");
        }
        //密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 账户不能重复
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(userQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 星球编号不能重复
        userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(userQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }
        //2.加密
        String encryptPassword = BCryptUtils.encrypt(userPassword);
        //3.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult;
        try {
            saveResult = this.save(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或星球编号已存在");
        }
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return user.getId();

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "账号或密码错误");
        }

        if (userAccount.length() < 4 || userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "账号或密码错误");
        }

        final String validPattern = "^[a-zA-Z0-9_.]+$";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);

        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "账号或密码错误");
        }
        // 查询用户是否存在
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        User user = userMapper.selectOne(userQueryWrapper);
        // 使用 BCrypt 校验明文密码，避免仅凭账号建立登录态
        if (user == null || !matchesUserPassword(userPassword, user)) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.NOT_LOGIN, "账号或密码错误");
        }
        if (user.getUserStatus() != null && user.getUserStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }
        //3.用户脱敏
        User safetyUser = getSafetyUser(user);
        //4.记录用户的登录态
        // Session 中只保存用户 id
        request.getSession().setAttribute(USER_LOGIN_STATE, user.getId());
        return safetyUser;

    }

    private boolean matchesUserPassword(String rawPassword, User user) {
        String storedPassword = user.getUserPassword();
        if (StringUtils.isBlank(storedPassword)) {
            return false;
        }
        if (storedPassword.startsWith("$2")) {
            return BCryptUtils.matches(rawPassword, storedPassword);
        }
        boolean matches = storedPassword.equals(rawPassword);
        if (matches) {
            User passwordMigration = new User();
            passwordMigration.setId(user.getId());
            passwordMigration.setUserPassword(BCryptUtils.encrypt(rawPassword));
            userMapper.updateById(passwordMigration);
        }
        return matches;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setUpdateTime(originUser.getUpdateTime());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setProfile(originUser.getProfile());
        return safetyUser;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取用户详细信息
     *
     * @param user
     * @return
     */
    @Override
    public UserDetailVO getUserDetailVO(User user) {
        //1. 用户为空直接返回
        if (user == null) {
            return null;
        }

        //2. 将 User 转换成 UserDetailVO
        UserDetailVO userDetailVO = new UserDetailVO();
        BeanUtils.copyProperties(user, userDetailVO);

        return userDetailVO;
    }

    /**
     * 获取用户详细信息列表
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserDetailVO> getUserDetailVOList(List<User> userList) {
        //1. 用户列表为空
        if (CollectionUtils.isEmpty(userList)) {
            return Collections.emptyList();
        }

        //2. 将 User 列表转换成 UserDetailVO 列表
        return userList.stream()
                .map(this::getUserDetailVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return Collections.emptyList();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public Page<UserVO> getUserVOPage(Page<User> userPage) {
        Page<UserVO> userVOPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        userVOPage.setRecords(getUserVOList(userPage.getRecords()));
        return userVOPage;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {

        request.getSession().invalidate();

        return 1;
    }

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList 用户要拥有的标签
     * @param pageNum     当前页
     * @param pageSize    每页大小
     * @return
     */
    @Override
    public Page<User> searchUsersByTags(List<String> tagNameList, long pageNum, long pageSize) {
        //1. 校验标签参数
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }

        //2. 校验分页参数
        if (pageNum <= 0 || pageSize <= 0 || pageSize > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }

        //3. 限制一次最多查询 10 个标签，防止恶意传入大量标签
        if (tagNameList.size() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "一次最多搜索 10 个标签");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        //4. 只查询 tags 不为空的用户
        queryWrapper.isNotNull("tags");
        queryWrapper.ne("tags", "");

        //5. 根据标签在数据库中进行过滤
        for (String tagName : tagNameList) {
            //标签不能为空
            if (StringUtils.isBlank(tagName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
            }

            //限制标签长度
            if (tagName.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签长度不能超过 20");
            }

            /**
             * 数据库中的 tags 格式：
             * ["Java","大二","男"]
             *
             * 如果搜索 Java，则查询：
             * tags like '%"Java"%'
             *
             * 加双引号的原因：
             * 如果直接 like '%Java%'，
             * 那么 "JavaScript" 也可能被错误匹配。
             */
            queryWrapper.like("tags", "\"" + tagName + "\"");
        }

        //6. 创建分页对象
        Page<User> userPage = new Page<>(pageNum, pageSize);

        //7. 由数据库进行过滤和分页，不再一次查询全部用户
        return this.page(userPage, queryWrapper);
    }

    @Override
    public int updateUser(UserUpdateRequest userUpdateRequest, User loginUser) {
        //1. 校验参数
        if (userUpdateRequest == null || userUpdateRequest.getId() == null || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long userId = userUpdateRequest.getId();

        //2. 如果是管理员，允许更新任意用户
        //如果不是管理员，只允许更新自己的信息
        if (!isAdmin(loginUser) && !Objects.equals(userId, loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        //3. 查询用户是否存在
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }

        //4. 校验 tags 是否为合法 JSON
        String tags = userUpdateRequest.getTags();

        if (StringUtils.isNotBlank(tags)) {
            validateTags(tags);
        }

        //5. 更新用户信息
        User updateUser = new User();
        BeanUtils.copyProperties(userUpdateRequest, updateUser);
        updateUser.setId(userId);

        return userMapper.updateById(updateUser);
    }


    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 请求不能为空
        if (request == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 2. 获取已有 Session
        // false 表示：如果 Session 不存在，不创建新的 Session
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 3. 从 Session 中获取用户 id
        Object userIdObj = session.getAttribute(USER_LOGIN_STATE);

        if (userIdObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 4. 将 Session 中的用户 id 转换成 Long
        Long userId;

        try {
            userId = Long.valueOf(userIdObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        if (userId <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 5. 根据用户 id 从数据库中重新查询用户
        User loginUser = this.getById(userId);

        // 用户可能已经被删除或者不存在
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 6. 返回当前登录用户
        return loginUser;
    }

    @Override
    public User getLoginUserOrNull(HttpServletRequest request) {

        if (request == null) {
            return null;
        }

        // 不存在 Session 时，不创建新的 Session
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        Object userIdObj =
                session.getAttribute(USER_LOGIN_STATE);

        if (userIdObj == null) {
            return null;
        }

        Long userId;

        try {
            userId = Long.valueOf(userIdObj.toString());
        } catch (NumberFormatException e) {
            return null;
        }

        if (userId <= 0) {
            return null;
        }

        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {

        User loginUser = getLoginUserOrNull(request);

        return loginUser != null
                && Integer.valueOf(ADMIN_ROLE)
                .equals(loginUser.getUserRole());
    }

    @Override
    public boolean isAdmin(User loginUser) {
        //仅管理员可查询
        return loginUser != null && Objects.equals(loginUser.getUserRole(), ADMIN_ROLE);
    }

    /**
     * 推荐匹配用户
     * <p>
     * 匹配规则：
     * 1. 至少拥有一个共同标签，否则不推荐
     * 2. 共同标签数量越多，优先级越高
     * 3. 共同标签数量相同时，Jaccard 相似度越高越优先
     * 4. 分批查询 + PriorityQueue，只保留 Top K，防止大量用户导致 OOM
     *
     * @param num       推荐人数
     * @param loginUser 当前登录用户
     * @return 匹配用户
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {

        // 1. 参数校验
        if (num <= 0 || loginUser == null) {
            return Collections.emptyList();
        }

        // 限制一次最多推荐 20 个用户
        int limit = (int) Math.min(num, 20);

        // 2. 获取当前登录用户标签
        String tags = loginUser.getTags();

        if (StringUtils.isBlank(tags)) {
            return Collections.emptyList();
        }

        // 3. 解析当前登录用户标签
        List<String> tagList = parseTags(tags);

        if (CollectionUtils.isEmpty(tagList)) {
            return Collections.emptyList();
        }

        // 转成 Set：
        // 方便后面计算交集、并集，同时自动去重
        Set<String> loginTagSet = new HashSet<>(tagList);

        // 去除可能存在的空标签
        loginTagSet.removeIf(StringUtils::isBlank);

        if (CollectionUtils.isEmpty(loginTagSet)) {
            return Collections.emptyList();
        }

        /*
         * 比较两个候选用户谁更匹配。
         *
         * 返回值 > 0：userMatch1 更好
         * 返回值 < 0：userMatch2 更好
         *
         * 第一优先级：共同标签数量
         * 第二优先级：Jaccard 相似度
         * 第三优先级：id 小的优先，只是为了让排序结果稳定
         */
        Comparator<UserMatch> matchComparator = (userMatch1, userMatch2) -> {

            // 1. 共同标签越多越好
            int commonCompare = Integer.compare(
                    userMatch1.getCommonTagCount(),
                    userMatch2.getCommonTagCount()
            );

            if (commonCompare != 0) {
                return commonCompare;
            }

            // 2. Jaccard 相似度越高越好
            int similarityCompare = Double.compare(
                    userMatch1.getSimilarity(),
                    userMatch2.getSimilarity()
            );

            if (similarityCompare != 0) {
                return similarityCompare;
            }

            // 3. 完全相同时，让 id 小的用户优先
            return Long.compare(
                    userMatch2.getUser().getId(),
                    userMatch1.getUser().getId()
            );
        };

        /*
         * PriorityQueue：
         *
         * 堆顶保存当前 Top K 中最差的那个用户。
         *
         * 这样每发现一个更好的候选用户，
         * 就把堆顶最差的删除。
         *
         * 队列中永远最多只有 limit 个对象，
         * 不会因为数据库有几十万、上百万用户而保存大量 UserMatch。
         */
        PriorityQueue<UserMatch> priorityQueue =
                new PriorityQueue<>(matchComparator);

        // 每批最多查询 1000 个候选用户
        final int batchSize = 1000;

        // 记录上一批最后一个用户 id
        long lastId = 0L;

        while (true) {

            // 4. 分批查询候选用户
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();

            queryWrapper
                    // 这里只查询匹配阶段真正需要的字段
                    .select("id", "tags")

                    // tags 不能为空
                    .isNotNull("tags")
                    .ne("tags", "")

                    // 使用 id 游标分页，避免 offset 越来越慢
                    .gt("id", lastId);

            /*
             * 5. 数据库层提前过滤：
             *
             * 当前用户假设拥有：
             * ["女", "找实习"]
             *
             * SQL 大致变成：
             *
             * AND (
             *     tags LIKE '%"女"%'
             *     OR tags LIKE '%"找实习"%'
             * )
             *
             * 因此：
             *
             * ["Python", "机器学习"]
             *
             * 一个共同标签都没有的用户，
             * 根本不会从数据库查询到 Java 中。
             */
            queryWrapper.and(qw -> {

                boolean first = true;

                for (String tag : loginTagSet) {

                    String searchTag = "\"" + tag + "\"";

                    if (first) {
                        qw.like("tags", searchTag);
                        first = false;
                    } else {
                        qw.or().like("tags", searchTag);
                    }
                }
            });

            // 按 id 顺序查询
            queryWrapper
                    .orderByAsc("id")
                    .last("limit " + batchSize);

            List<User> userList = this.list(queryWrapper);

            // 已经没有候选用户
            if (CollectionUtils.isEmpty(userList)) {
                break;
            }

            // 6. 计算这一批用户的相似度
            for (User user : userList) {

                /*
                 * 注意：
                 * lastId 必须更新。
                 *
                 * 下一轮只查询 id > lastId 的用户。
                 */
                lastId = Math.max(lastId, user.getId());

                // 排除自己
                if (Objects.equals(user.getId(), loginUser.getId())) {
                    continue;
                }

                String userTags = user.getTags();

                if (StringUtils.isBlank(userTags)) {
                    continue;
                }

                List<String> userTagList = parseTags(userTags);

                if (CollectionUtils.isEmpty(userTagList)) {
                    continue;
                }

                Set<String> userTagSet = new HashSet<>(userTagList);
                userTagSet.removeIf(StringUtils::isBlank);

                if (CollectionUtils.isEmpty(userTagSet)) {
                    continue;
                }

                /*
                 * 7. 求交集
                 *
                 * 例如：
                 *
                 * 当前用户：
                 * 女、找实习
                 *
                 * 候选用户：
                 * 女、找实习、Java
                 *
                 * 交集：
                 * 女、找实习
                 */
                Set<String> intersection = new HashSet<>(loginTagSet);

                intersection.retainAll(userTagSet);

                int commonTagCount = intersection.size();

                /*
                 * 一个共同标签都没有：
                 *
                 * 直接不推荐。
                 *
                 * 这样 Python + 机器学习
                 * 就不会推荐给 女 + 找实习。
                 */
                if (commonTagCount == 0) {
                    continue;
                }

                /*
                 * 8. 求并集
                 */
                Set<String> union = new HashSet<>(loginTagSet);

                union.addAll(userTagSet);

                /*
                 * 9. 计算 Jaccard 相似度
                 *
                 * 相似度 = 交集数量 / 并集数量
                 *
                 * 范围：
                 * 0 ~ 1
                 *
                 * 越接近 1，标签越相似。
                 */
                double similarity =
                        (double) commonTagCount / union.size();

                UserMatch userMatch = new UserMatch(
                        user,
                        commonTagCount,
                        similarity
                );

                /*
                 * 10. PriorityQueue 只保存最好的 limit 个
                 */
                if (priorityQueue.size() < limit) {

                    // 数量还没满，直接加入
                    priorityQueue.offer(userMatch);

                } else {

                    // 当前队列中最差的那个用户
                    UserMatch worstUser = priorityQueue.peek();

                    /*
                     * 当前用户比最差用户更匹配：
                     *
                     * 删除最差的
                     * 加入当前用户
                     */
                    if (matchComparator.compare(userMatch, worstUser) > 0) {

                        priorityQueue.poll();

                        priorityQueue.offer(userMatch);
                    }
                }
            }

            /*
             * 本批不足 1000 个，
             * 说明已经查询到最后一批。
             */
            if (userList.size() < batchSize) {
                break;
            }
        }

        // 没有任何拥有共同标签的用户
        if (priorityQueue.isEmpty()) {
            return Collections.emptyList();
        }

        /*
         * 11. PriorityQueue 本身不是完整有序的，
         * 转成 List 后再按照：
         *
         * 共同标签数 ↓
         * Jaccard    ↓
         *
         * 排序。
         */
        List<UserMatch> topUserList =
                new ArrayList<>(priorityQueue);

        topUserList.sort(matchComparator.reversed());

        // 12. 获取已经排好序的 userId
        List<Long> userIdList =
                topUserList.stream()
                        .map(item -> item.getUser().getId())
                        .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(userIdList)) {
            return Collections.emptyList();
        }

        /*
         * 13. 前面为了节省内存只查询了：
         *
         * id
         * tags
         *
         * 现在只查询最终 Top K 用户的完整信息。
         */
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();

        userQueryWrapper.in("id", userIdList);

        Map<Long, User> userMap =
                this.list(userQueryWrapper)
                        .stream()
                        .map(this::getSafetyUser)
                        .collect(Collectors.toMap(
                                User::getId,
                                user -> user,
                                (user1, user2) -> user1
                        ));

        /*
         * 14. MySQL 的 IN 查询不保证返回顺序，
         * 所以按照前面已经排好的 userIdList
         * 重新组成最终结果。
         */
        List<User> finalUserList = new ArrayList<>();

        for (Long userId : userIdList) {

            User user = userMap.get(userId);

            if (user != null) {
                finalUserList.add(user);
            }
        }

        return finalUserList;
    }

    /**
     * 删除用户
     *
     * @param userId    要删除的用户 id
     * @param loginUser 当前登录用户
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(long userId, User loginUser) {

        // 1. 校验用户 id
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 校验登录状态
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 3. 只有管理员能够删除用户
        if (!isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        // 4. 查询准备删除的用户
        User user = this.getById(userId);

        if (user == null) {
            throw new BusinessException(
                    ErrorCode.NULL_ERROR,
                    "用户不存在"
            );
        }

        /*
         * 5. 判断该用户是否还是某个队伍的队长
         *
         * 这里不要调用 TeamService，
         * 否则：
         *
         * UserServiceImpl -> TeamServiceImpl
         * TeamServiceImpl -> UserServiceImpl
         *
         * 会形成循环依赖。
         *
         * 这里只是做简单数据库查询，
         * 直接使用 TeamMapper 即可。
         */
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();

        teamQueryWrapper.eq("userId", userId);

        Long createdTeamCount =
                teamMapper.selectCount(teamQueryWrapper);

        // 6. 如果用户仍然是队长，不允许直接删除
        if (createdTeamCount > 0) {
            throw new BusinessException(
                    ErrorCode.PARAMS_ERROR,
                    "该用户仍然是队伍队长，请先处理其创建的队伍"
            );
        }

        /*
         * 7. 删除这个用户与其他队伍的关联关系
         *
         * 例如：
         *
         * userId = 5
         * teamId = 1
         *
         * userId = 5
         * teamId = 3
         *
         * 删除用户之前先删除这些 user_team 数据。
         */
        QueryWrapper<UserTeam> userTeamQueryWrapper =
                new QueryWrapper<>();

        userTeamQueryWrapper.eq("userId", userId);

        userTeamMapper.delete(userTeamQueryWrapper);

        // 8. 最后删除用户
        return this.removeById(userId);
    }


    /**
     * 根据标签搜索用户(SQL 查询版)
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
//    @Deprecated
//    private List<User> searchUsersByTagsBySql(List<String> tagNameList) {
//        if (CollectionUtils.isEmpty(tagNameList)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        for (String tagName : tagNameList) {
//            queryWrapper.like("tags", tagName);
//        }
//        List<User> userList = userMapper.selectList(queryWrapper);
//        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
//    }


    /**
     * 解析标签
     */
    private List<String> parseTags(String tagsStr) {
        if (StringUtils.isBlank(tagsStr)) {
            return new ArrayList<>();
        }

        try {
            Gson gson = new Gson();

            List<String> tagList = gson.fromJson(tagsStr, new TypeToken<List<String>>() {
                    }.getType()
            );

            if (tagList == null) {
                return new ArrayList<>();
            }

            return tagList;

        } catch (Exception e) {
            log.warn("用户标签解析失败，tags = {}", tagsStr);
            return new ArrayList<>();
        }
    }

    /**
     * 校验标签
     */
    private void validateTags(String tagsStr) {
        //1. 标签为空不校验
        if (StringUtils.isBlank(tagsStr)) {
            return;
        }

        try {
            //2. 将 JSON 转换成标签集合
            Gson gson = new Gson();

            List<String> tagList = gson.fromJson(
                    tagsStr,
                    new TypeToken<List<String>>() {
                    }.getType()
            );

            if (tagList == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签格式错误");
            }

            //3. 标签数量不能过多
            if (tagList.size() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签数量不能超过 20 个");
            }

            //4. 标签不能重复
            Set<String> tagSet = new HashSet<>(tagList);

            if (tagSet.size() != tagList.size()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能重复");
            }

            //5. 校验每一个标签
            for (String tag : tagList) {
                if (StringUtils.isBlank(tag)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
                }

                if (tag.length() > 20) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "单个标签长度不能超过 20");
                }
            }

        } catch (BusinessException e) {
            //业务异常直接抛出
            throw e;

        } catch (Exception e) {
            //JSON 格式异常统一转成参数错误
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签必须是正确的 JSON 数组格式");
        }
    }

    /**
     * 用户标签匹配结果
     */
    private static class UserMatch {

        /**
         * 候选用户
         */
        private final User user;

        /**
         * 与当前登录用户共同拥有的标签数量
         */
        private final int commonTagCount;

        /**
         * Jaccard 相似度
         * <p>
         * 范围：0 ~ 1
         */
        private final double similarity;

        private UserMatch(
                User user,
                int commonTagCount,
                double similarity
        ) {
            this.user = user;
            this.commonTagCount = commonTagCount;
            this.similarity = similarity;
        }

        private User getUser() {
            return user;
        }

        private int getCommonTagCount() {
            return commonTagCount;
        }

        private double getSimilarity() {
            return similarity;
        }
    }
}





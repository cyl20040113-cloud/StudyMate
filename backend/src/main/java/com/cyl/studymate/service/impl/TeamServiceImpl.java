package com.cyl.studymate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyl.studymate.common.ErrorCode;
import com.cyl.studymate.common.exception.BusinessException;
import com.cyl.studymate.mapper.TeamMapper;
import com.cyl.studymate.model.domain.Team;
import com.cyl.studymate.model.domain.User;
import com.cyl.studymate.model.domain.UserTeam;
import com.cyl.studymate.model.dto.TeamQuery;
import com.cyl.studymate.model.enums.TeamStatusEnum;
import com.cyl.studymate.model.request.TeamJoinRequest;
import com.cyl.studymate.model.request.TeamQuitRequest;
import com.cyl.studymate.model.request.TeamUpdateRequest;
import com.cyl.studymate.model.vo.TeamVO;
import com.cyl.studymate.model.vo.UserVO;
import com.cyl.studymate.service.TeamService;
import com.cyl.studymate.service.UserService;
import com.cyl.studymate.service.UserTeamService;
import com.cyl.studymate.utils.BCryptUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 队伍服务实现。
 */
@Service
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    private static final int MAX_TEAM_NUM = 5;

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public long addTeam(Team team, User loginUser) {
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        validateNewTeam(team);

        long userId = loginUser.getId();
        team.setId(null);
        team.setUserId(userId);
        if (TeamStatusEnum.SECRET.getValue() == team.getStatus()) {
            team.setPassword(BCryptUtils.encrypt(team.getPassword()));
        } else {
            team.setPassword(null);
        }

        RLock lock = redissonClient.getLock(userMembershipLockKey(userId));
        boolean locked = false;
        try {
            // 先加锁，再开启事务；事务提交完成后才会执行 finally 解锁。
            locked = lock.tryLock(3, -1, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
            }

            Long teamId = transactionTemplate.execute(status -> doAddTeam(team, userId));
            if (teamId == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
            }
            return teamId;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    @Override
    public List<TeamVO> listTeams(TeamQuery teamQuery, User loginUser) {
        TeamQuery safeQuery = teamQuery == null ? new TeamQuery() : teamQuery;
        if (safeQuery.getIdList() != null && safeQuery.getIdList().isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<Team> queryWrapper = buildTeamQueryWrapper(safeQuery, loginUser);
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        return convertToTeamVOList(this.list(queryWrapper));
    }

    @Override
    public Page<TeamVO> listTeamPage(TeamQuery teamQuery, User loginUser) {
        if (teamQuery == null || teamQuery.getPageNum() < 1 || teamQuery.getPageSize() < 1
                || teamQuery.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数不合法");
        }
        if (teamQuery.getIdList() != null && teamQuery.getIdList().isEmpty()) {
            return new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize(), 0);
        }
        QueryWrapper<Team> queryWrapper = buildTeamQueryWrapper(teamQuery, loginUser);
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        Page<Team> teamPage = this.page(new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize()), queryWrapper);
        Page<TeamVO> result = new Page<>(teamPage.getCurrent(), teamPage.getSize(), teamPage.getTotal());
        result.setRecords(convertToTeamVOList(teamPage.getRecords()));
        return result;
    }

    @Override
    public TeamVO getTeamVO(long id, User loginUser) {
        Team team = getRequiredTeam(id);
        if (TeamStatusEnum.PRIVATE.getValue() == team.getStatus() && !canAccessPrivateTeam(team, loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())
                && !canManageTeam(team, loginUser)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        return convertToTeamVOList(Collections.singletonList(team)).get(0);
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest request, User loginUser) {
        // 1. 校验请求参数
        if (request == null || request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        Long teamId = request.getId();

        // 2. 获取当前队伍对应的分布式锁
        // updateTeam、joinTeam、quitTeam、deleteTeam 操作同一个队伍时使用同一把锁，
        // 避免修改队伍状态的同时还有用户加入、退出或者删除队伍。
        RLock teamLock = redissonClient.getLock(teamOperationLockKey(teamId));
        boolean locked = false;

        try {
            // 3. 尝试获取锁
            // 最多等待 3 秒；
            // leaseTime = -1 表示启用 Redisson Watch Dog 自动续期，
            // 防止业务还没有执行完成锁就提前释放。
            locked = teamLock.tryLock(3, -1, TimeUnit.SECONDS);

            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
            }

            /*
             * 4. 一定要在获取锁以后重新查询队伍。
             *
             * 如果在获取锁之前查询：
             *
             * 查询 oldTeam
             *     ↓
             * 等待获取锁
             *     ↓
             * 其他线程修改队伍
             *     ↓
             * 当前线程获得锁
             *
             * 此时 oldTeam 就已经是旧数据。
             */
            Team oldTeam = getRequiredTeam(teamId);

            // 5. 判断当前用户是否有修改队伍的权限
            // 当前逻辑：队长或者管理员可以修改队伍。
            if (!canManageTeam(oldTeam, loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }

            /*
             * 6. 计算修改后的最终字段值。
             *
             * 如果前端没有传某个字段，就继续使用数据库中的旧值。
             * 这样可以支持局部更新。
             */
            String effectiveName = request.getName() == null
                    ? oldTeam.getName()
                    : request.getName();

            String effectiveDescription = request.getDescription() == null
                    ? oldTeam.getDescription()
                    : request.getDescription();

            Integer effectiveStatus = request.getStatus() == null
                    ? oldTeam.getStatus()
                    : request.getStatus();

            // 7. 校验队伍的公共字段
            validateCommonFields(
                    effectiveName,
                    effectiveDescription,
                    oldTeam.getMaxNum(),
                    effectiveStatus
            );

            // 8. 如果修改了过期时间，新过期时间必须晚于当前时间
            if (request.getExpireTime() != null
                    && !request.getExpireTime().after(new Date())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间必须晚于当前时间");
            }

            // 9. 获取修改后的队伍状态
            TeamStatusEnum statusEnum =
                    TeamStatusEnum.getEnumByValue(effectiveStatus);

            String newPassword = request.getPassword();

            /*
             * 10. 如果修改后的队伍是加密队伍，需要检查密码。
             */
            if (TeamStatusEnum.SECRET.equals(statusEnum)) {

                /*
                 * 判断原来的队伍是不是已经是加密队伍，
                 * 并且数据库中已经存在加密后的密码。
                 */
                boolean alreadySecret =
                        TeamStatusEnum.SECRET.getValue() == oldTeam.getStatus()
                                && StringUtils.isNotBlank(oldTeam.getPassword());

                /*
                 * 情况：
                 *
                 * PUBLIC / PRIVATE -> SECRET
                 *
                 * 此时原来没有密码，所以必须提供新密码。
                 *
                 * 如果原来已经是 SECRET，而且没有修改密码，
                 * 则继续使用旧密码即可。
                 */
                if (StringUtils.isBlank(newPassword) && !alreadySecret) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍必须设置密码");
                }

                // 队伍密码最长 32 位
                if (StringUtils.isNotBlank(newPassword)
                        && newPassword.length() > 32) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能超过 32 位");
                }
            }

            /*
             * 11. 创建更新条件。
             *
             * 不直接 BeanUtils.copyProperties，
             * 避免前端没有传递的字段被错误更新成 null。
             */
            UpdateWrapper<Team> updateWrapper = new UpdateWrapper<>();

            updateWrapper.eq("id", teamId);

            boolean changed = false;

            // 12. 修改队伍名称
            if (request.getName() != null) {
                updateWrapper.set("name", request.getName());
                changed = true;
            }

            // 13. 修改队伍描述
            if (request.getDescription() != null) {
                updateWrapper.set(
                        "description",
                        request.getDescription()
                );
                changed = true;
            }

            // 14. 修改过期时间
            if (request.getExpireTime() != null) {
                updateWrapper.set(
                        "expireTime",
                        request.getExpireTime()
                );
                changed = true;
            }

            // 15. 修改队伍状态
            if (request.getStatus() != null) {
                updateWrapper.set(
                        "status",
                        request.getStatus()
                );
                changed = true;
            }

            /*
             * 16. 处理队伍密码。
             *
             * SECRET：
             * 如果传入了新密码，就 BCrypt 加密以后保存。
             */
            if (TeamStatusEnum.SECRET.equals(statusEnum)
                    && StringUtils.isNotBlank(newPassword)) {

                updateWrapper.set(
                        "password",
                        BCryptUtils.encrypt(newPassword)
                );

                changed = true;
            }

            /*
             * 如果修改后的状态不是 SECRET，
             * 并且此次请求修改了 status，
             * 则清空之前可能存在的队伍密码。
             *
             * 例如：
             *
             * SECRET -> PUBLIC
             *
             * password 必须清空。
             */
            else if (!TeamStatusEnum.SECRET.equals(statusEnum)
                    && request.getStatus() != null) {

                updateWrapper.set("password", null);

                changed = true;
            }

            /*
             * 17. 如果用户没有修改任何字段，则直接返回 true。
             *
             * 否则执行数据库 UPDATE。
             */
            return !changed || this.update(updateWrapper);

        } catch (InterruptedException e) {

            /*
             * tryLock() 可能因为线程被中断抛出 InterruptedException。
             *
             * 恢复线程的中断状态，
             * 不要直接把中断状态吞掉。
             */
            Thread.currentThread().interrupt();

            throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "操作被中断"
            );

        } finally {

            /*
             * 18. 释放分布式锁。
             *
             * 必须同时判断：
             *
             * 1. 当前线程成功获得过锁
             * 2. 当前线程目前仍然持有锁
             *
             * 防止错误 unlock。
             */
            if (locked && teamLock.isHeldByCurrentThread()) {
                teamLock.unlock();
            }
        }
    }

    @Override
    public boolean joinTeam(TeamJoinRequest request, User loginUser) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long teamId = request.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long userId = loginUser.getId();
        RLock userLock = redissonClient.getLock(userMembershipLockKey(userId));
        RLock teamLock = redissonClient.getLock(teamOperationLockKey(teamId));
        RLock lock = redissonClient.getMultiLock(userLock, teamLock);
        boolean locked = false;

        try {
            // 用户锁保证“最多 5 个队伍”的判断与写入串行化；
            // 队伍锁保证队伍容量、退出、解散等成员变更串行化。
            locked = lock.tryLock(3, -1, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
            }

            Boolean result = transactionTemplate.execute(status -> doJoinTeam(request, loginUser));
            return Boolean.TRUE.equals(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作被中断");
        } finally {
            // execute 正常返回时事务已经提交；异常时事务已经回滚。
            // 因此这里释放锁时，数据库事务一定已经结束。
            if (locked) {
                lock.unlock();
            }
        }
    }


    @Override
    public boolean quitTeam(TeamQuitRequest request, User loginUser) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long teamId = request.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long userId = loginUser.getId();
        RLock userLock = redissonClient.getLock(userMembershipLockKey(userId));
        RLock teamLock = redissonClient.getLock(teamOperationLockKey(teamId));
        RLock lock = redissonClient.getMultiLock(userLock, teamLock);
        boolean locked = false;

        try {
            locked = lock.tryLock(3, -1, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
            }

            Boolean result = transactionTemplate.execute(status -> doQuitTeam(request, loginUser));
            return Boolean.TRUE.equals(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作被中断");
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }


    @Override
    public boolean deleteTeam(long id, User loginUser) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        RLock teamLock = redissonClient.getLock(teamOperationLockKey(id));
        boolean locked = false;
        try {
            locked = teamLock.tryLock(3, -1, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
            }

            Boolean result = transactionTemplate.execute(status -> doDeleteTeam(id, loginUser));
            return Boolean.TRUE.equals(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "操作被中断");
        } finally {
            if (locked && teamLock.isHeldByCurrentThread()) {
                teamLock.unlock();
            }
        }
    }


    /**
     * 创建队伍的数据库事务部分。
     * 调用它之前必须已经持有当前用户的 membership 锁。
     */
    private Long doAddTeam(Team team, long userId) {
        QueryWrapper<UserTeam> membershipQuery = new QueryWrapper<>();
        membershipQuery.eq("userId", userId);
        if (userTeamService.count(membershipQuery) >= MAX_TEAM_NUM) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
        }

        if (!this.save(team) || team.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }

        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(team.getId());
        userTeam.setJoinTime(new Date());
        try {
            if (!userTeamService.save(userTeam)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
        }
        return team.getId();
    }

    /**
     * 加入队伍的数据库事务部分。
     * 调用它之前必须同时持有 user membership 锁和 team operation 锁。
     */
    private boolean doJoinTeam(TeamJoinRequest request, User loginUser) {
        Long teamId = request.getTeamId();
        long userId = loginUser.getId();

        // 必须在拿到锁后重新查询，避免使用加锁前的旧数据。
        Team team = getRequiredTeam(teamId);
        validateJoinableTeam(team, request.getPassword());

        QueryWrapper<UserTeam> userMembershipQuery = new QueryWrapper<>();
        userMembershipQuery.eq("userId", userId);
        if (userTeamService.count(userMembershipQuery) >= MAX_TEAM_NUM) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
        }

        QueryWrapper<UserTeam> duplicateQuery = new QueryWrapper<>();
        duplicateQuery.eq("userId", userId).eq("teamId", teamId);
        if (userTeamService.count(duplicateQuery) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
        }

        if (countTeamUserByTeamId(teamId) >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
        }

        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        try {
            if (!userTeamService.save(userTeam)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加入队伍失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
        }
        return true;
    }

    /**
     * 退出队伍的数据库事务部分。
     */
    private boolean doQuitTeam(TeamQuitRequest request, User loginUser) {
        Long teamId = request.getTeamId();
        long userId = loginUser.getId();
        Team team = getRequiredTeam(teamId);

        QueryWrapper<UserTeam> relationQuery = new QueryWrapper<>();
        relationQuery.eq("userId", userId).eq("teamId", teamId);
        if (userTeamService.count(relationQuery) == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }

        long teamUserCount = countTeamUserByTeamId(teamId);
        if (teamUserCount <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍成员数据异常");
        }

        if (teamUserCount == 1) {
            // 最后一名成员退出：先删除成员关系，再删除队伍。
            if (!userTeamService.remove(relationQuery)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍失败");
            }
            if (!this.removeById(teamId)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
            }
            return true;
        }

        if (Objects.equals(team.getUserId(), userId)) {
            QueryWrapper<UserTeam> nextLeaderQuery = new QueryWrapper<>();
            nextLeaderQuery.eq("teamId", teamId)
                    .ne("userId", userId)
                    .orderByAsc("joinTime", "id")
                    .last("limit 1");

            UserTeam nextLeader = userTeamService.getOne(nextLeaderQuery);
            if (nextLeader == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法转移队长");
            }

            Team updateTeam = new Team();
            updateTeam.setId(teamId);
            updateTeam.setUserId(nextLeader.getUserId());
            if (!this.updateById(updateTeam)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
            }
        }

        if (!userTeamService.remove(relationQuery)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍失败");
        }
        return true;
    }

    /**
     * 解散队伍的数据库事务部分。
     */
    private boolean doDeleteTeam(long id, User loginUser) {
        Team team = getRequiredTeam(id);
        if (!canManageTeam(team, loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无访问权限");
        }

        QueryWrapper<UserTeam> relationQuery = new QueryWrapper<>();
        relationQuery.eq("teamId", id);
        long relationCount = userTeamService.count(relationQuery);
        if (relationCount > 0 && !userTeamService.remove(relationQuery)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍成员关系失败");
        }

        if (!this.removeById(id)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        }
        return true;
    }

    private void validateNewTeam(Team team) {
        int status = Optional.ofNullable(team.getStatus()).orElse(TeamStatusEnum.PUBLIC.getValue());
        validateCommonFields(team.getName(), team.getDescription(), team.getMaxNum(), status);
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.getValue() == status && StringUtils.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍必须设置密码");
        }
        if (StringUtils.isNotBlank(password) && password.length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能超过 32 位");
        }
        if (team.getExpireTime() != null && !team.getExpireTime().after(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "过期时间必须晚于当前时间");
        }
        team.setStatus(status);
    }

    private void validateCommonFields(String name, String description, Integer maxNum, Integer status) {
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称不能为空且不能超过 20 位");
        }
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述不能超过 512 位");
        }
        if (maxNum == null || maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数必须在 1 到 20 之间");
        }
        if (TeamStatusEnum.getEnumByValue(status) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不合法");
        }
    }

    private void validateJoinableTeam(Team team, String rawPassword) {
        if (team.getExpireTime() != null && team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        TeamStatusEnum status = TeamStatusEnum.getEnumByValue(team.getStatus());
        if (status == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "队伍状态异常");
        }
        if (TeamStatusEnum.PRIVATE.equals(status)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "禁止加入私有队伍");
        }
        if (TeamStatusEnum.SECRET.equals(status)
                && (StringUtils.isBlank(rawPassword) || !matchesTeamPassword(rawPassword, team))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
    }

    private boolean matchesTeamPassword(String rawPassword, Team team) {
        String storedPassword = team.getPassword();
        if (StringUtils.isBlank(storedPassword)) {
            return false;
        }
        if (storedPassword.startsWith("$2")) {
            return BCryptUtils.matches(rawPassword, storedPassword);
        }
        boolean matches = storedPassword.equals(rawPassword);
        if (matches) {
            Team migration = new Team();
            migration.setId(team.getId());
            migration.setPassword(BCryptUtils.encrypt(rawPassword));
            this.updateById(migration);
        }
        return matches;
    }

    private QueryWrapper<Team> buildTeamQueryWrapper(TeamQuery query, User loginUser) {
        QueryWrapper<Team> wrapper = new QueryWrapper<>();
        if (query.getId() != null && query.getId() > 0) {
            wrapper.eq("id", query.getId());
        }
        if (CollectionUtils.isNotEmpty(query.getIdList())) {
            wrapper.in("id", query.getIdList());
        }
        if (StringUtils.isNotBlank(query.getSearchText())) {
            wrapper.and(qw -> qw.like("name", query.getSearchText())
                    .or().like("description", query.getSearchText()));
        }
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like("name", query.getName());
        }
        if (StringUtils.isNotBlank(query.getDescription())) {
            wrapper.like("description", query.getDescription());
        }
        if (query.getMaxNum() != null && query.getMaxNum() > 0) {
            wrapper.eq("maxNum", query.getMaxNum());
        }
        if (query.getUserId() != null && query.getUserId() > 0) {
            wrapper.eq("userId", query.getUserId());
        }
        applyVisibilityFilter(wrapper, query.getStatus(), loginUser);
        return wrapper;
    }

    private void applyVisibilityFilter(QueryWrapper<Team> wrapper, Integer requestedStatus, User loginUser) {
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(requestedStatus);
        if (requestedStatus != null && statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不合法");
        }
        if (userService.isAdmin(loginUser)) {
            if (statusEnum != null) {
                wrapper.eq("status", statusEnum.getValue());
            }
            return;
        }

        List<Long> joinedTeamIds = getJoinedTeamIds(loginUser);
        if (TeamStatusEnum.PRIVATE.equals(statusEnum)) {
            if (loginUser == null) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            wrapper.eq("status", TeamStatusEnum.PRIVATE.getValue())
                    .and(access -> {
                        access.eq("userId", loginUser.getId());
                        if (!joinedTeamIds.isEmpty()) {
                            access.or().in("id", joinedTeamIds);
                        }
                    });
            return;
        }
        if (statusEnum != null) {
            wrapper.eq("status", statusEnum.getValue());
            return;
        }

        wrapper.and(visible -> {
            visible.in("status", Arrays.asList(TeamStatusEnum.PUBLIC.getValue(), TeamStatusEnum.SECRET.getValue()));
            if (loginUser != null) {
                visible.or(privateTeam -> privateTeam.eq("status", TeamStatusEnum.PRIVATE.getValue())
                        .and(access -> {
                            access.eq("userId", loginUser.getId());
                            if (!joinedTeamIds.isEmpty()) {
                                access.or().in("id", joinedTeamIds);
                            }
                        }));
            }
        });
    }

    private List<Long> getJoinedTeamIds(User loginUser) {
        if (loginUser == null) {
            return Collections.emptyList();
        }
        QueryWrapper<UserTeam> query = new QueryWrapper<>();
        query.eq("userId", loginUser.getId()).select("teamId");
        return userTeamService.list(query).stream()
                .map(UserTeam::getTeamId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<TeamVO> convertToTeamVOList(List<Team> teams) {
        if (CollectionUtils.isEmpty(teams)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = teams.stream().map(Team::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, UserVO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userService.listByIds(userIds)) {
                userMap.put(user.getId(), userService.getUserVO(user));
            }
        }
        List<TeamVO> result = new ArrayList<>(teams.size());
        for (Team team : teams) {
            TeamVO teamVO = new TeamVO();
            BeanUtils.copyProperties(team, teamVO);
            teamVO.setCreateUser(userMap.get(team.getUserId()));
            result.add(teamVO);
        }
        return result;
    }

    private boolean canAccessPrivateTeam(Team team, User loginUser) {
        if (canManageTeam(team, loginUser)) {
            return true;
        }
        if (loginUser == null) {
            return false;
        }
        QueryWrapper<UserTeam> query = new QueryWrapper<>();
        query.eq("teamId", team.getId()).eq("userId", loginUser.getId());
        return userTeamService.count(query) > 0;
    }

    private boolean canManageTeam(Team team, User loginUser) {
        return loginUser != null && (Objects.equals(team.getUserId(), loginUser.getId())
                || userService.isAdmin(loginUser));
    }

    private Team getRequiredTeam(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    private long countTeamUserByTeamId(Long teamId) {
        QueryWrapper<UserTeam> query = new QueryWrapper<>();
        query.eq("teamId", teamId);
        return userTeamService.count(query);
    }

    private String userMembershipLockKey(long userId) {
        return "studymate:team:membership:user:" + userId;
    }

    private String teamOperationLockKey(long teamId) {
        return "studymate:team:operation:" + teamId;
    }
}

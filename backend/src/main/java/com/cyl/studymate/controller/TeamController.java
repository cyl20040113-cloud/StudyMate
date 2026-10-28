package com.cyl.studymate.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyl.studymate.common.BaseResponse;
import com.cyl.studymate.common.DeleteRequest;
import com.cyl.studymate.common.ErrorCode;
import com.cyl.studymate.common.ResultUtils;
import com.cyl.studymate.common.exception.BusinessException;
import com.cyl.studymate.model.domain.Team;
import com.cyl.studymate.model.domain.User;
import com.cyl.studymate.model.domain.UserTeam;
import com.cyl.studymate.model.dto.TeamQuery;
import com.cyl.studymate.model.request.TeamAddRequest;
import com.cyl.studymate.model.request.TeamJoinRequest;
import com.cyl.studymate.model.request.TeamQuitRequest;
import com.cyl.studymate.model.request.TeamUpdateRequest;
import com.cyl.studymate.model.vo.TeamVO;
import com.cyl.studymate.service.TeamService;
import com.cyl.studymate.service.UserService;
import com.cyl.studymate.service.UserTeamService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest request, HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        Team team = new Team();
        BeanUtils.copyProperties(request, team);
        return ResultUtils.success(teamService.addTeam(team, loginUser));
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest request, HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        if (!teamService.updateTeam(request, loginUser)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<TeamVO> getTeamById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(teamService.getTeamVO(id, userService.getLoginUserOrNull(request)));
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUserOrNull(request);
        List<TeamVO> teamList = teamService.listTeams(teamQuery, loginUser);
        enrichMembershipData(teamList, loginUser);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<TeamVO>> listPageTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUserOrNull(request);
        Page<TeamVO> resultPage = teamService.listTeamPage(teamQuery, loginUser);
        enrichMembershipData(resultPage.getRecords(), loginUser);
        return ResultUtils.success(resultPage);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest request, HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(teamService.joinTeam(request, userService.getLoginUser(httpRequest)));
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest request, HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(teamService.quitTeam(request, userService.getLoginUser(httpRequest)));
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest,
                                            HttpServletRequest request) {
        // 1. 校验请求参数
        if (deleteRequest == null
                || deleteRequest.getId() == null
                || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 3. 删除队伍
        boolean result = teamService.deleteTeam(deleteRequest.getId(), loginUser);

        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        }

        return ResultUtils.success(true);
    }

    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamVO>> listMyCreateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamVO> teamList = teamService.listTeams(teamQuery, loginUser);
        enrichMembershipData(teamList, loginUser);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId()).select("teamId");
        List<Long> teamIds = userTeamService.list(queryWrapper).stream()
                .map(UserTeam::getTeamId)
                .distinct()
                .collect(Collectors.toList());
        if (teamIds.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        teamQuery.setIdList(teamIds);
        List<TeamVO> teamList = teamService.listTeams(teamQuery, loginUser);
        enrichMembershipData(teamList, loginUser);
        return ResultUtils.success(teamList);
    }

    private void enrichMembershipData(List<TeamVO> teamList, User loginUser) {
        if (teamList == null || teamList.isEmpty()) {
            return;
        }
        List<Long> teamIds = teamList.stream().map(TeamVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> query = new QueryWrapper<>();
        query.in("teamId", teamIds);
        List<UserTeam> relations = userTeamService.list(query);
        Map<Long, Long> memberCounts = relations.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId, Collectors.counting()));
        Set<Long> joinedTeamIds = loginUser == null ? Collections.emptySet() : relations.stream()
                .filter(relation -> Objects.equals(relation.getUserId(), loginUser.getId()))
                .map(UserTeam::getTeamId)
                .collect(Collectors.toSet());
        teamList.forEach(team -> {
            team.setHasJoin(joinedTeamIds.contains(team.getId()));
            team.setHasJoinNum(memberCounts.getOrDefault(team.getId(), 0L).intValue());
        });
    }
}

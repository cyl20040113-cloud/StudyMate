package com.cyl.studymate.service;

import com.cyl.studymate.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyl.studymate.model.domain.User;
import com.cyl.studymate.model.dto.TeamQuery;
import com.cyl.studymate.model.request.TeamJoinRequest;
import com.cyl.studymate.model.request.TeamQuitRequest;
import com.cyl.studymate.model.request.TeamUpdateRequest;
import com.cyl.studymate.model.vo.TeamVO;

import java.util.List;

/**
* @author 31893
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2026-08-20 15:28:07
*/
public interface TeamService extends IService<Team> {

    /**
     *  添加队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍
     * @param teamQuery
     * @param loginUser
     * @return
     */
    List<TeamVO> listTeams(TeamQuery teamQuery, User loginUser);

    Page<TeamVO> listTeamPage(TeamQuery teamQuery, User loginUser);

    TeamVO getTeamVO(long id, User loginUser);

    /**
     * 更新队伍
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     *  加入队伍
     * @param teamJoinRequest
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 删除（解散）队伍
     * @param id
     * @return
     */
    boolean deleteTeam(long id, User loginUser);
}

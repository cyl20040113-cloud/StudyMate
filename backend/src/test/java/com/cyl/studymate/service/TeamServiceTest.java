package com.cyl.studymate.service;

import com.cyl.studymate.common.ErrorCode;
import com.cyl.studymate.common.exception.BusinessException;
import com.cyl.studymate.model.domain.Team;
import com.cyl.studymate.model.domain.User;
import com.cyl.studymate.model.domain.UserTeam;
import com.cyl.studymate.model.enums.TeamStatusEnum;
import com.cyl.studymate.model.request.TeamUpdateRequest;
import com.cyl.studymate.utils.BCryptUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeamServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserTeamService userTeamService;

    @MockBean
    private RedissonClient redissonClient;

    private RLock lock;

    @BeforeEach
    void setUpLock() throws InterruptedException {
        lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(3, -1, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    void publicTeamCanBeCreatedWithoutPasswordOrExpireTime() {
        User loginUser = normalUser(1L);
        Team team = new Team();
        team.setName("public-team");
        team.setMaxNum(5);
        team.setStatus(TeamStatusEnum.PUBLIC.getValue());

        long teamId = teamService.addTeam(team, loginUser);
        Team storedTeam = teamService.getById(teamId);

        assertEquals(TeamStatusEnum.PUBLIC.getValue(), storedTeam.getStatus());
        assertNull(storedTeam.getPassword());
        assertNull(storedTeam.getExpireTime());
    }

    @Test
    void secretTeamPasswordIsHashed() {
        Team team = new Team();
        team.setName("secret-team");
        team.setMaxNum(5);
        team.setStatus(TeamStatusEnum.SECRET.getValue());
        team.setPassword("team-password");

        long teamId = teamService.addTeam(team, normalUser(2L));
        String storedPassword = teamService.getById(teamId).getPassword();

        assertTrue(BCryptUtils.matches("team-password", storedPassword));
    }

    @Test
    void invalidStatusIsRejectedDuringUpdate() {
        User loginUser = normalUser(3L);
        Team team = new Team();
        team.setName("update-team");
        team.setMaxNum(5);
        team.setStatus(TeamStatusEnum.PUBLIC.getValue());
        long teamId = teamService.addTeam(team, loginUser);

        TeamUpdateRequest request = new TeamUpdateRequest();
        request.setId(teamId);
        request.setStatus(99);
        BusinessException exception = assertThrows(BusinessException.class,
                () -> teamService.updateTeam(request, loginUser));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void userWithFiveMembershipsCannotCreateSixthTeam() {
        User loginUser = normalUser(4L);
        for (long teamId = 100; teamId < 105; teamId++) {
            UserTeam relation = new UserTeam();
            relation.setUserId(loginUser.getId());
            relation.setTeamId(teamId);
            userTeamService.save(relation);
        }
        Team team = new Team();
        team.setName("sixth-team");
        team.setMaxNum(5);
        team.setStatus(TeamStatusEnum.PUBLIC.getValue());

        assertThrows(BusinessException.class, () -> teamService.addTeam(team, loginUser));
    }

    private User normalUser(long id) {
        User user = new User();
        user.setId(id);
        user.setUserRole(0);
        return user;
    }
}

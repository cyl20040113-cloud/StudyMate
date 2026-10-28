package com.cyl.studymate.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyl.studymate.model.domain.UserTeam;
import com.cyl.studymate.service.UserTeamService;
import com.cyl.studymate.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author 31893
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2026-08-20 15:31:33
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}





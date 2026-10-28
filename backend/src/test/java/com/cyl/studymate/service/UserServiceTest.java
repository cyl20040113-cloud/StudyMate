package com.cyl.studymate.service;

import com.cyl.studymate.common.ErrorCode;
import com.cyl.studymate.common.exception.BusinessException;
import com.cyl.studymate.model.domain.User;
import com.cyl.studymate.model.request.UserUpdateRequest;
import com.cyl.studymate.utils.BCryptUtils;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private RedissonClient redissonClient;

    @Test
    void registerStoresBcryptAndCorrectPasswordCanLogin() {
        long userId = userService.userRegister("test_user", "12345678", "12345678", "10001");
        User storedUser = userService.getById(userId);

        assertNotEquals("12345678", storedUser.getUserPassword());
        assertTrue(BCryptUtils.matches("12345678", storedUser.getUserPassword()));

        User loginUser = userService.userLogin("test_user", "12345678", new MockHttpServletRequest());
        assertEquals(userId, loginUser.getId());
    }

    @Test
    void wrongPasswordCannotLogin() {
        userService.userRegister("login_user", "12345678", "12345678", "10002");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.userLogin("login_user", "wrong-pass", new MockHttpServletRequest()));
        assertEquals(ErrorCode.NOT_LOGIN.getCode(), exception.getCode());
    }

    @Test
    void normalUpdateCannotChangeRoleOrPassword() {
        long userId = userService.userRegister("update_user", "12345678", "12345678", "10003");
        User loginUser = userService.getSafetyUser(userService.getById(userId));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setId(userId);
        request.setUsername("new-name");
        assertEquals(1, userService.updateUser(request, loginUser));

        User updatedUser = userService.getById(userId);
        assertEquals("new-name", updatedUser.getUsername());
        assertEquals(0, updatedUser.getUserRole());
        assertTrue(BCryptUtils.matches("12345678", updatedUser.getUserPassword()));
    }

    @Test
    void invalidRegistrationThrowsBusinessException() {
        assertThrows(BusinessException.class,
                () -> userService.userRegister("abc", "12345678", "12345678", "10004"));
        assertThrows(BusinessException.class,
                () -> userService.userRegister("valid_user", "12345678", "different", "10005"));
    }
}

package com.example.pfa.reservation;

import com.example.pfa.reservation.jwt.CustomerUsersDetailsService;
import com.example.pfa.reservation.model.User;
import com.example.pfa.reservation.repository.UserDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerUsersDetailsServiceTest {

    @Mock
    private UserDAO userDao;

    @InjectMocks
    private CustomerUsersDetailsService service;

    @Test
    void loadUserByUsername_userFound_returnsUserDetails() {
        User u = new User();
        u.setEmail("test@example.com");
        u.setPassword("encodedPass");

        when(userDao.findByEmailOrUsername("test@example.com", "test@example.com")).thenReturn(u);

        UserDetails ud = service.loadUserByUsername("test@example.com");

        assertNotNull(ud);
        assertEquals("test@example.com", ud.getUsername());
        assertEquals("encodedPass", ud.getPassword());
        verify(userDao, times(1)).findByEmailOrUsername("test@example.com", "test@example.com");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
        when(userDao.findByEmailOrUsername("nope", "nope")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nope"));

        verify(userDao, times(1)).findByEmailOrUsername("nope", "nope");
    }
}

package com.example.pfa.reservation.jwt;

import com.example.pfa.reservation.repository.UserDAO;
import com.example.pfa.reservation.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;

@Slf4j
@Service
public class CustomerUsersDetailsService implements UserDetailsService {

    private final UserDAO userDao;

    // Injection par constructeur → conforme à SonarQube
    public CustomerUsersDetailsService(UserDAO userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
        log.info("Inside loadUserByUsername {}", emailOrUsername);

        User userDetail = userDao.findByEmailOrUsername(emailOrUsername, emailOrUsername);

        if (!Objects.isNull(userDetail)) {
            return new org.springframework.security.core.userdetails.User(
                    userDetail.getEmail(),
                    userDetail.getPassword(),
                    new ArrayList<>()
            );
        } else {
            throw new UsernameNotFoundException("User not found.");
        }
    }

}

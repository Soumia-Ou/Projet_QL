package com.example.pfa.reservation.JWT;

import com.example.pfa.reservation.Repository.userDAO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;

@Slf4j //logging propre et efficace
@Service
public class CustomerUsersDetailsService implements UserDetailsService {

    @Autowired
    userDAO  userDao;


    private com.example.pfa.reservation.model.user userDetail;

    @Override
    public UserDetails loadUserByUsername(String EmailOrUsername) throws UsernameNotFoundException {
        log.info("Inside loadUserByUsername {}",EmailOrUsername);
        // Recherche de l'utilisateur dans la base de données
        userDetail = userDao.findByEmailOrUsername(EmailOrUsername, EmailOrUsername);

        if(!Objects.isNull(userDetail))
            return new User(userDetail.getEmail(),userDetail.getPassword(),new ArrayList<>());
        else
           throw new UsernameNotFoundException("User not found .");
    }

    public com.example.pfa.reservation.model.user getUserDetail(){
        return userDetail;
    }

}

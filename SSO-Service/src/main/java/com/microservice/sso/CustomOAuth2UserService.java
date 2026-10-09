package com.microservice.sso;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private SsoUserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Let Spring Boot fetch the data from GitHub automatically
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String login = oAuth2User.getAttribute("login");
        String name = oAuth2User.getAttribute("name");
        Object idObj = oAuth2User.getAttribute("id");
        String providerId = idObj != null ? idObj.toString() : "unknown";

        // 2. Just-In-Time Provisioning
        Optional<SsoUser> existingUser = userRepository.findByProviderId(providerId);
        SsoUser dbUser;
        
        if (existingUser.isEmpty()) {
            dbUser = new SsoUser();
            dbUser.setName(name != null ? name : login);
            dbUser.setEmail(login + "@github.com");
            dbUser.setOauthProvider("GITHUB");
            dbUser.setProviderId(providerId);
            
            // 3. Assign RBAC Role! 
            // If it is your specific GitHub account, give you the ADMIN role! Otherwise, USER.
            if (login != null && login.equalsIgnoreCase("AshishUjjwal")) {
                dbUser.setRole("ROLE_ADMIN");
            } else {
                dbUser.setRole("ROLE_USER");
            }
            
            dbUser = userRepository.save(dbUser);
        } else {
            dbUser = existingUser.get();
        }

        // 4. Attach the permanent Database Role to the Spring Security Session!
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(dbUser.getRole()));

        // We return a new OAuth2User object that has our custom Database Roles attached to it.
        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "login");
    }
}

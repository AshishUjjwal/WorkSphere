package com.microservice.sso;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "<h1>Welcome! Please Login.</h1>";
        }
        
        String name = principal.getAttribute("name");
        String login = principal.getAttribute("login"); 
        
        // Let's print out what Role Spring Security has officially assigned you!
        String authorities = principal.getAuthorities().toString();

        Map<String, Object> allData = principal.getAttributes();
        
        return "<h1>🎉 Welcome to Enterprise SSO, " + name + " (" + login + ")!</h1>" +
               "<h3 style='color:purple'>Your Official Assigned Roles: " + authorities + "</h3>" +
               "<h4><a href='/admin'>Click here to test the Secret Admin Dashboard</a></h4>" +
               "<hr>" +
               "<p>Here is all the secret data GitHub safely provided to us:</p>" +
               "<pre>" + allData.toString() + "</pre>";
    }

    // This endpoint is protected by Role-Based Access Control (RBAC)!
    @GetMapping("/admin")
    public String adminDashboard(@AuthenticationPrincipal OAuth2User principal) {
        return "<h1 style='color:red'>🚨 TOP SECRET ADMIN DASHBOARD 🚨</h1>" +
               "<h3>Welcome, Admin " + principal.getAttribute("name") + ". You have authorization to view this page.</h3>" +
               "<br><a href='/'>Go Back</a>";
    }
}

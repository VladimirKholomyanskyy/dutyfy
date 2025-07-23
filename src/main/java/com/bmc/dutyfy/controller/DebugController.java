package com.bmc.dutyfy.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DebugController {

    @GetMapping("/debug/auth")
    @ResponseBody
    public String debugAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return "Authenticated: " + auth.getName() + " | Authorities: " + auth.getAuthorities();
        }
        return "Not authenticated";
    }

    @PostMapping("/debug/login")
    @ResponseBody
    public String debugLogin(@RequestParam String username, @RequestParam String password) {
        return "Received login attempt - Username: " + username + " | Password length: " + password.length();
    }
}
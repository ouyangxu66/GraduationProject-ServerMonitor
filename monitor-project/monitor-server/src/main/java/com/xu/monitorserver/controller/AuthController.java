package com.xu.monitorserver.controller;


import com.xu.monitorcommon.dto.LoginDTO;
import com.xu.monitorcommon.dto.RegisterDTO;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.service.authservice.IAuthService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginDTO loginDTO) {
        String token = authService.login(loginDTO.getUsername(), loginDTO.getPassword());
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return R.ok(result);
    }
    @PostMapping("/register")
    public R<Void> register(@RequestBody RegisterDTO dto) {
        authService.register(dto);
        return R.ok();
    }
}
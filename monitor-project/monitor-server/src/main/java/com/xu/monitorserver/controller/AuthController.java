package com.xu.monitorserver.controller;


import com.xu.monitorcommon.dto.LoginDTO;
import com.xu.monitorcommon.dto.RegisterDTO;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.service.authservice.IAuthService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    // 修改登录返回类型为 Map
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody LoginDTO loginDTO) {
        // 调用修改后的 login 方法
        Map<String, String> tokens = authService.login(loginDTO.getUsername(), loginDTO.getPassword());
        return R.ok(tokens);
    }

    // 🟢 新增：刷新接口
    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(@RequestBody Map<String, String> params) {
        String refreshToken = params.get("refreshToken");
        Map<String, String> tokens = authService.refreshToken(refreshToken);
        return R.ok(tokens);
    }
    @PostMapping("/register")
    public R<Void> register(@RequestBody RegisterDTO dto) {
        authService.register(dto);
        return R.ok();
    }
}
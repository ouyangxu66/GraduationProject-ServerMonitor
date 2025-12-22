package com.xu.monitorserver.controller;

import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.dto.SshConfigResponse;
import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.service.serverservice.IServerInfoService;
import com.xu.monitorserver.service.serverservice.SshSecretCryptoService;
import com.xu.monitorserver.service.sshservice.SshTicket;
import com.xu.monitorserver.service.sshservice.SshTicketService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/server")
public class ServerInfoController {

    private final IServerInfoService serverInfoService;

    private final SshTicketService sshTicketService;

    private final SshSecretCryptoService cryptoService;

    public ServerInfoController(IServerInfoService serverInfoService,
                                SshTicketService sshTicketService,
                                SshSecretCryptoService cryptoService) {
        this.serverInfoService = serverInfoService;
        this.sshTicketService = sshTicketService;
        this.cryptoService = cryptoService;
    }

    /**
     * 获取所有服务器列表
     * GET /server/list
     */
    @GetMapping("/list")
    public R<List<ServerInfo>> list() {
        String username = getCurrentUsername();
        return R.ok(serverInfoService.listForUser(username));
    }

    /**
     * 终端页自动连接用配置接口（ticket 方案）。
     *
     * <p>返回内容不包含 password/privateKey/passphrase 明文，仅返回：
     * host/port/username/authPreferred/hasPrivateKey/fingerprint/sshTicket。
     * 前端在 WebSocket connect 报文带上 sshTicket 即可。</p>
     */
    @GetMapping("/{id}/ssh-config")
    public R<SshConfigResponse> sshConfig(@PathVariable("id") Long id) {
        String username = getCurrentUsername();

        ServerInfo server = serverInfoService.getById(id);
        if (server == null) {
            throw new ServiceException(404, "服务器不存在");
        }
        if (server.getCreateBy() == null || !server.getCreateBy().equals(username)) {
            throw new ServiceException(403, "无权访问此服务器");
        }

        boolean hasKey = server.hasPrivateKeyConfigured() && server.getPrivateKeyEnc() != null && !server.getPrivateKeyEnc().isBlank();
        String authPreferred = hasKey ? "publicKey" : "password";

        String plaintextPrivateKey = hasKey ? cryptoService.decryptFromBase64(server.getPrivateKeyEnc()) : null;
        String plaintextPassphrase = hasKey ? cryptoService.decryptFromBase64(server.getKeyPassphraseEnc()) : null;
        String plaintextPassword = (!hasKey && server.getPassword() != null) ? cryptoService.decryptFromBase64(server.getPassword()) : null;

        int port = server.getPort() != null ? server.getPort() : 22;

        SshTicket ticket = new SshTicket(
                server.getId(),
                server.getIp(),
                port,
                server.getUsername(),
                authPreferred,
                plaintextPassword,
                plaintextPrivateKey,
                plaintextPassphrase,
                Instant.now().plusSeconds(60)
        );
        String sshTicket = sshTicketService.issue(username, ticket);

        SshConfigResponse resp = new SshConfigResponse();
        resp.setServerId(server.getId());
        resp.setHost(server.getIp());
        resp.setPort(port);
        resp.setUsername(server.getUsername());
        resp.setAuthPreferred(authPreferred);
        resp.setHasPrivateKey(hasKey);
        resp.setFingerprint(server.getPrivateKeyFingerprint());
        resp.setSshTicket(sshTicket);
        return R.ok(resp);
    }

    /**
     * 新增或更新服务器
     * POST /server/save
     */
    @PostMapping("/save")
    public R<String> save(@RequestBody ServerInfo serverInfo) {
        String username = getCurrentUsername();
        serverInfoService.saveForUser(username, serverInfo);
        return R.ok("保存成功");
    }

    /**
     * 删除服务器
     * DELETE /server/1
     */
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable("id") Long id) {
        String username = getCurrentUsername();
        serverInfoService.deleteForUser(username, id);
        return R.ok("删除成功");
    }

    /**
     * 辅助方法:获取当前登录用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ServiceException(401, "未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String s) {
            // Spring Security 未登录时通常为 anonymousUser
            if ("anonymousUser".equalsIgnoreCase(s)) {
                throw new ServiceException(401, "未登录");
            }
            return s;
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new ServiceException(401, "未登录");
    }
}
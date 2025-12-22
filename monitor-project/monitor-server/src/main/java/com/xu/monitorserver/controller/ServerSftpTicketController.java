package com.xu.monitorserver.controller;

import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.dto.sftp.SftpTicketResponse;
import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.service.serverservice.IServerInfoService;
import com.xu.monitorserver.service.serverservice.SshSecretCryptoService;
import com.xu.monitorserver.service.sftp.SftpTicket;
import com.xu.monitorserver.service.sftp.SftpTicketService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/server")
public class ServerSftpTicketController {

    private final IServerInfoService serverInfoService;
    private final SftpTicketService sftpTicketService;
    private final SshSecretCryptoService cryptoService;

    public ServerSftpTicketController(IServerInfoService serverInfoService,
                                      SftpTicketService sftpTicketService,
                                      SshSecretCryptoService cryptoService) {
        this.serverInfoService = serverInfoService;
        this.sftpTicketService = sftpTicketService;
        this.cryptoService = cryptoService;
    }

    /**
     * 获取 SFTP 文件操作 ticket。
     *
     * <p>返回内容不包含 password/privateKey/passphrase 明文，仅返回一次性 ticket。</p>
     */
    @GetMapping("/{id}/sftp-ticket")
    public R<SftpTicketResponse> sftpTicket(@PathVariable("id") Long id) {
        String username = getCurrentUsername();

        ServerInfo server = serverInfoService.getById(id);
        if (server == null) {
            throw new ServiceException(404, "服务器不存在");
        }
        if (server.getCreateBy() == null || !server.getCreateBy().equals(username)) {
            throw new ServiceException(403, "无权访问此服务器");
        }

        boolean hasKey = server.hasPrivateKeyConfigured()
                && server.getPrivateKeyEnc() != null
                && !server.getPrivateKeyEnc().isBlank();
        String authPreferred = hasKey ? "publicKey" : "password";

        String plaintextPrivateKey = hasKey ? cryptoService.decryptFromBase64(server.getPrivateKeyEnc()) : null;
        String plaintextPassphrase = hasKey ? cryptoService.decryptFromBase64(server.getKeyPassphraseEnc()) : null;
        String plaintextPassword = (!hasKey && server.getPassword() != null)
                ? cryptoService.decryptFromBase64(server.getPassword())
                : null;

        int port = server.getPort() != null ? server.getPort() : 22;

        Instant expiresAt = Instant.now().plusSeconds(60);
        SftpTicket ticket = new SftpTicket(
                server.getId(),
                server.getIp(),
                port,
                server.getUsername(),
                authPreferred,
                plaintextPassword,
                plaintextPrivateKey,
                plaintextPassphrase,
                expiresAt
        );
        String sftpTicket = sftpTicketService.issue(username, ticket);

        SftpTicketResponse resp = new SftpTicketResponse();
        resp.setServerId(server.getId());
        resp.setSftpTicket(sftpTicket);
        resp.setExpiresAt(expiresAt);

        return R.ok(resp);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ServiceException(401, "未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String s) {
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

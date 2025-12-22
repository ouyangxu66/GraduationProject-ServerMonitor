package com.xu.monitorserver.controller;

import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.dto.sftp.SftpListItem;
import com.xu.monitorserver.dto.sftp.SftpUploadResponse;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.service.sftp.SftpErrorRegistry;
import com.xu.monitorserver.service.sftp.SftpService;
import com.xu.monitorserver.service.sftp.SftpTicket;
import com.xu.monitorserver.service.sftp.SftpTicketService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/sftp")
public class SftpController {

    private final SftpTicketService ticketService;
    private final SftpService sftpService;

    public SftpController(SftpTicketService ticketService, SftpService sftpService) {
        this.ticketService = ticketService;
        this.sftpService = sftpService;
    }

    @GetMapping("/list")
    public R<List<SftpListItem>> list(@RequestParam("ticket") String ticket,
                                     @RequestParam(value = "path", required = false) String path) {
        String username = getCurrentUsername();
        SftpTicket t = ticketService.consume(username, ticket);
        if (t == null) {
            throw new ServiceException(401, SftpErrorRegistry.userMessageOf(SftpErrorRegistry.SFTP_TICKET_INVALID));
        }
        return R.ok(sftpService.list(t, path));
    }

    @PostMapping("/upload")
    public R<SftpUploadResponse> upload(@RequestParam("ticket") String ticket,
                                        @RequestParam("targetDir") String targetDir,
                                        @RequestParam(value = "overwrite", required = false, defaultValue = "false") boolean overwrite,
                                        @RequestParam("file") MultipartFile file) {
        String username = getCurrentUsername();
        SftpTicket t = ticketService.consume(username, ticket);
        if (t == null) {
            throw new ServiceException(401, SftpErrorRegistry.userMessageOf(SftpErrorRegistry.SFTP_TICKET_INVALID));
        }

        try {
            var result = sftpService.uploadFromStream(t, targetDir, file.getOriginalFilename(), file.getInputStream(), overwrite);
            SftpUploadResponse resp = new SftpUploadResponse();
            resp.setRemotePath(result.remotePath());
            resp.setSize(result.size());
            return R.ok(resp);
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            throw new ServiceException(500, "上传失败");
        }
    }

    @GetMapping("/download")
    public void download(@RequestParam("ticket") String ticket,
                         @RequestParam("path") String path,
                         HttpServletResponse response) {
        String username = getCurrentUsername();
        SftpTicket t = ticketService.consume(username, ticket);
        if (t == null) {
            throw new ServiceException(401, SftpErrorRegistry.userMessageOf(SftpErrorRegistry.SFTP_TICKET_INVALID));
        }

        try {
            String filename = path == null ? "download" : path;
            filename = filename.replace('\\', '/');
            int idx = filename.lastIndexOf('/');
            if (idx >= 0) filename = filename.substring(idx + 1);
            if (filename.isBlank()) filename = "download";

            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/octet-stream");
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);

            try (OutputStream out = response.getOutputStream()) {
                sftpService.downloadToStream(t, path, out);
                out.flush();
            }
        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            throw new ServiceException(500, "下载失败");
        }
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

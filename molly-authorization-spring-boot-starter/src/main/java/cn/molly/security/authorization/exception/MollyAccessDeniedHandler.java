package cn.molly.security.authorization.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * 鉴权失败场景的统一处理器，输出 403 JSON 响应。
 * <p>
 * 使用者可通过提供同名 Bean 覆盖默认实现，例如接入全站统一响应体或审计日志。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class MollyAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 写出 403 响应。
     *
     * @param request               当前请求
     * @param response              当前响应
     * @param accessDeniedException 鉴权失败异常
     * @throws IOException 写出异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        JsonErrorResponseWriter.write(request, response, HttpStatus.FORBIDDEN, accessDeniedException.getMessage());
    }
}

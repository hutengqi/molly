package cn.molly.security.authorization.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * 未认证 / Token 无效场景的统一入口，输出 401 JSON 响应。
 * <p>
 * 使用者可通过提供同名 Bean 覆盖默认实现，例如接入全站统一响应体。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class MollyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 写出 401 响应。
     *
     * @param request       当前请求
     * @param response      当前响应
     * @param authException 认证异常（包含原始错误描述）
     * @throws IOException 写出异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        JsonErrorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, authException.getMessage());
    }
}

package cn.molly.security.authorization.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

/**
 * 统一 JSON 错误响应写出工具。
 * <p>
 * 为避免强依赖 Jackson，采用手写 JSON 的方式生成最简响应体：
 * <pre>{"timestamp":"...","status":401,"error":"Unauthorized","message":"...","path":"..."}</pre>
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public final class JsonErrorResponseWriter {

    private JsonErrorResponseWriter() {
    }

    /**
     * 将指定的 HTTP 状态与错误消息以 JSON 格式写入响应。
     *
     * @param request  当前请求，用于回填 {@code path} 字段
     * @param response 当前响应
     * @param status   HTTP 状态码
     * @param message  错误描述，允许为 null
     * @throws IOException 写出异常
     */
    public static void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String body = "{"
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + escape(status.getReasonPhrase()) + "\","
                + "\"message\":\"" + escape(message == null ? "" : message) + "\","
                + "\"path\":\"" + escape(request.getRequestURI()) + "\""
                + "}";

        try (PrintWriter writer = response.getWriter()) {
            writer.write(body);
            writer.flush();
        }
    }

    /**
     * 对 JSON 字符串值中的特殊字符做最小化转义。
     *
     * @param raw 原始字符串
     * @return 转义后的字符串
     */
    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}

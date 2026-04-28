package cn.molly.example.resource.controller;

import cn.molly.security.authorization.rbac.annotation.MollyPreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 示例用户接口，用于验证 {@code @MollyPreAuthorize} 的鉴权效果。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    /**
     * 列表接口，要求具备 {@code user:read} 权限。
     *
     * @return 示例数据
     */
    @GetMapping
    @MollyPreAuthorize(perm = "user:read")
    public List<Map<String, Object>> list() {
        return List.of(
                Map.of("id", 1, "name", "Alice"),
                Map.of("id", 2, "name", "Bob")
        );
    }

    /**
     * 创建接口，要求具备 {@code user:write} 权限。
     *
     * @return 占位响应
     */
    @PostMapping
    @MollyPreAuthorize(perm = "user:write")
    public Map<String, Object> create() {
        return Map.of("created", true);
    }

    /**
     * 导出接口，要求同时具备读 / 导出两项权限。
     *
     * @return 占位响应
     */
    @GetMapping("/export")
    @MollyPreAuthorize(allPerm = {"user:read", "user:export"})
    public Map<String, Object> export() {
        return Map.of("exported", true);
    }
}

package cn.molly.example.resource.controller;

import cn.molly.security.authorization.rbac.annotation.MollyPreAuthorize;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * resource profile 下的示例用户接口，用于验证 {@code @MollyPreAuthorize} 的鉴权效果。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/29
 */
@Profile("resource")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    @MollyPreAuthorize(perm = "user:read")
    public List<Map<String, Object>> list() {
        return List.of(
                Map.of("id", 1, "name", "Alice"),
                Map.of("id", 2, "name", "Bob")
        );
    }

    @PostMapping
    @MollyPreAuthorize(perm = "user:write")
    public Map<String, Object> create() {
        return Map.of("created", true);
    }

    @GetMapping("/export")
    @MollyPreAuthorize(allPerm = {"user:read", "user:export"})
    public Map<String, Object> export() {
        return Map.of("exported", true);
    }
}

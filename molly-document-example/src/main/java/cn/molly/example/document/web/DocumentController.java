package cn.molly.example.document.web;

import cn.molly.document.email.MailRequest;
import cn.molly.document.email.MailService;
import cn.molly.document.excel.ExcelTemplate;
import cn.molly.document.pdf.PdfTemplate;
import cn.molly.document.word.WordTemplate;
import cn.molly.example.document.model.OrderRow;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 四类文档能力演示端点。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@RestController
@RequestMapping("/doc")
@RequiredArgsConstructor
public class DocumentController {

    private final ObjectProvider<WordTemplate> wordTemplate;
    private final ObjectProvider<ExcelTemplate> excelTemplate;
    private final ObjectProvider<PdfTemplate> pdfTemplate;
    private final ObjectProvider<MailService> mailService;

    /**
     * Word：按模板渲染。
     */
    @PostMapping("/word/{name}")
    public void word(@org.springframework.web.bind.annotation.PathVariable String name,
                     @RequestBody Map<String, Object> model,
                     HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=" + name);
        try (OutputStream out = response.getOutputStream()) {
            wordTemplate.getObject().render(name, model, out);
        }
    }

    /**
     * Excel：导出订单列表（演示）。
     */
    @GetMapping("/excel/orders")
    public void excelOrders(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=orders.xlsx");
        List<OrderRow> rows = new ArrayList<>();
        rows.add(new OrderRow("SO202604280001", "张三", "13800000000",
                new BigDecimal("199.90"), OrderRow.OrderStatus.PAID, LocalDateTime.now()));
        rows.add(new OrderRow("SO202604280002", "李四", "13900000000",
                new BigDecimal("58.00"), OrderRow.OrderStatus.CREATED, LocalDateTime.now()));
        try (OutputStream out = response.getOutputStream()) {
            excelTemplate.getObject().export(OrderRow.class, rows, out);
        }
    }

    /**
     * PDF：按 AcroForm 模板填充。
     */
    @PostMapping("/pdf/{name}")
    public void pdf(@org.springframework.web.bind.annotation.PathVariable String name,
                    @RequestBody Map<String, String> fields,
                    HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + name);
        try (OutputStream out = response.getOutputStream()) {
            pdfTemplate.getObject().render(name, fields, out);
        }
    }

    /**
     * Email：异步发送 HTML 邮件（演示）。
     */
    @PostMapping("/mail/demo")
    public String mail(@RequestBody Map<String, String> body) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", body.getOrDefault("name", "Molly"));
        MailRequest req = MailRequest.builder()
                .to(List.of(body.get("to")))
                .subject("Molly Document Starter Demo")
                .template("welcome")
                .templateVariables(vars)
                .html(true)
                .build();
        mailService.getObject().sendAsync(req);
        return "accepted";
    }
}

package cn.molly.example.document;

import cn.molly.document.core.TemplateLoader;
import cn.molly.document.pdf.DefaultPdfTemplate;
import cn.molly.document.pdf.PdfTemplate;
import cn.molly.document.properties.MollyDocumentProperties;
import cn.molly.document.word.DefaultWordTemplate;
import cn.molly.document.word.WordTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地冒烟入口：一次性渲染 contract.docx / invoice.pdf 两个示例模板到 target/ 目录。
 * 运行方式：mvn -pl molly-document-example exec:java -Dexec.mainClass=...TemplateSmokeMain
 *
 * @author Ht7_Sincerity
 * @since 2026/4/29
 */
public class TemplateSmokeMain {

    public static void main(String[] args) throws Exception {
        Path outDir = Paths.get("target", "smoke");
        Files.createDirectories(outDir);

        // ---- Word ----
        TemplateLoader wordLoader = new TemplateLoader("classpath:/templates/word/", true);
        WordTemplate word = new DefaultWordTemplate(wordLoader);

        Map<String, Object> model = new HashMap<>();
        model.put("contractNo", "MOLLY-2026-0429-0001");
        model.put("partyA", "示例科技有限公司");
        model.put("partyB", "Molly 开源团队");
        model.put("signDate", "2026-04-29");
        model.put("serviceName", "文档生成 Starter 技术支持");
        model.put("serviceDesc", "Word/Excel/PDF/Email 四类文档生成能力咨询与落地");
        model.put("amount", "50,000.00");
        model.put("amountCn", "伍万元整");
        model.put("prepay", Boolean.TRUE);
        model.put("paymentTerms", "甲方应在合同签订后 7 日内预付款项。");
        model.put("partyASigner", "张三");
        model.put("partyBSigner", "Ht7_Sincerity");

        model.put("item1Name", "技术咨询");
        model.put("item1Qty", "1 人月");
        model.put("item1Price", "30,000.00");
        model.put("item2Name", "现场支持");
        model.put("item2Qty", "5 人日");
        model.put("item2Price", "15,000.00");
        model.put("item3Name", "技术文档");
        model.put("item3Qty", "1 套");
        model.put("item3Price", "5,000.00");

        Path wordOut = outDir.resolve("contract-rendered.docx");
        try (var os = Files.newOutputStream(wordOut)) {
            word.render("contract.docx", model, os);
        }
        System.out.println("[OK] Word => " + wordOut.toAbsolutePath() + " (" + Files.size(wordOut) + " bytes)");

        // ---- PDF ----
        TemplateLoader pdfLoader = new TemplateLoader("classpath:/templates/pdf/", true);
        PdfTemplate pdf = new DefaultPdfTemplate(pdfLoader, new MollyDocumentProperties.Pdf());

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("invoiceNo", "INV-202604290001");
        fields.put("issueDate", "2026-04-29");
        fields.put("customer", "Example Tech Co., Ltd.");
        fields.put("phone", "+86 138-0000-0000");
        fields.put("email", "ap@example.com");
        fields.put("address", "Shanghai, China");
        fields.put("item1Name", "Consulting");
        fields.put("item1Qty", "1");
        fields.put("item1Price", "30000.00");
        fields.put("item1Amount", "30000.00");
        fields.put("item2Name", "Onsite support");
        fields.put("item2Qty", "5");
        fields.put("item2Price", "3000.00");
        fields.put("item2Amount", "15000.00");
        fields.put("subtotal", "45000.00");
        fields.put("tax", "2700.00");
        fields.put("total", "47700.00");
        fields.put("remark", "Payable within 30 days.");

        Path pdfOut = outDir.resolve("invoice-filled.pdf");
        try (var os = Files.newOutputStream(pdfOut)) {
            pdf.render("invoice.pdf", fields, os);
        }
        System.out.println("[OK] PDF  => " + pdfOut.toAbsolutePath() + " (" + Files.size(pdfOut) + " bytes)");
    }
}

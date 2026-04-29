# PDF 模板目录

本目录用于放置带 AcroForm 表单字段的 PDF 模板，由 `molly-document-spring-boot-starter` 基于 [PDFBox](https://pdfbox.apache.org/) 进行字段填充。

## 制作流程

1. 使用 Adobe Acrobat / LibreOffice Draw / WPS PDF 等工具打开目标 PDF
2. 开启「表单 / Form」模式，在需要填值的位置放置「文本域 (Text Field)」
3. 为每个文本域设置唯一 `Name`（该名称将作为渲染时的 key）
4. 保存为 `.pdf` 放入本目录（例如 `invoice.pdf`）

## 约定

- 编码：UTF-8（中文请在 `molly.document.pdf.font.chinese` 指定字体资源）
- 模板根路径：`molly.document.pdf.template-location`（默认 `classpath:/templates/pdf/`）
- 填充入口：`PdfTemplate#render(templateName, fields, out)`，`fields` 为 `Map<String,String>`，key 与 PDF 表单域 Name 一致
- Flatten：`molly.document.pdf.flatten=true` 时，填充后表单不可再编辑（建议开启）

## 调用示例

本目录已预置 `invoice.pdf`（发票模板，共 22 个 AcroForm 文本域），字段 Name 如下：

| 分组 | 字段 |
|------|------|
| 发票元信息 | `invoiceNo`, `issueDate` |
| 客户信息 | `customer`, `phone`, `email`, `address` |
| 明细（i ∈ 1..3）| `item{i}Name`, `item{i}Qty`, `item{i}Price`, `item{i}Amount` |
| 合计 | `subtotal`, `tax`, `total` |
| 备注 | `remark`（多行）|

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"invoiceNo":"INV-202604290001","customer":"Example Tech Co., Ltd.","total":"47700.00"}' \
  http://localhost:8081/doc/pdf/invoice.pdf \
  -o invoice-filled.pdf
```

或直接运行本地冒烟入口：

```bash
mvn -pl molly-document-example -am -DskipTests install
cd molly-document-example && mvn dependency:build-classpath -Dmdep.outputFile=target/cp.txt -DincludeScope=runtime
java -cp "target/classes:$(cat target/cp.txt)" cn.molly.example.document.TemplateSmokeMain
# 输出在 target/smoke/invoice-filled.pdf
```

## 模板重新生成

仅供维护者参考：模板由 `scripts/gen_invoice_pdf.py` 生成（依赖 `reportlab`）：

```bash
python3 -m pip install reportlab
python3 scripts/gen_invoice_pdf.py
```

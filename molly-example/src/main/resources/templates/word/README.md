# Word 模板目录

本目录用于放置 `.docx` 格式的 Word 模板，由 `molly-document-spring-boot-starter` 基于 [poi-tl](https://deepoove.com/poi-tl/) 语法渲染。

## 模板语法速览

在 Word 文档中直接使用占位符：

| 占位符 | 语义 | 示例 |
|------|------|------|
| `{{var}}` | 文本 | `{{name}}` |
| `{{?section}}...{{/section}}` | 条件块 | `{{?vip}}尊贵会员{{/vip}}` |
| `{{*list}}` | 区段循环 | `{{*items}}{{name}}-{{price}}{{/items}}` |
| `{{@img}}` | 图片（需 `PictureRenderData`） | `{{@signature}}` |

## 约定

- 编码：UTF-8
- 模板根路径：`molly.document.word.template-location`（默认 `classpath:/templates/word/`）
- 渲染入口：`WordTemplate#render(templateName, model, out)`，其中 `templateName` 为相对路径（如 `contract.docx`）

## 调用示例

本目录已预置 `contract.docx`（服务合同模板），它声明了如下占位符：

| 占位符 | 说明 |
|------|------|
| `{{contractNo}}` | 合同编号 |
| `{{partyA}}` / `{{partyB}}` | 甲方 / 乙方 |
| `{{signDate}}` | 签订日期 |
| `{{serviceName}}` / `{{serviceDesc}}` | 服务名称 / 说明 |
| `{{amount}}` / `{{amountCn}}` | 金额（小写 / 大写）|
| `{{item{i}Name}}` / `{{item{i}Qty}}` / `{{item{i}Price}}` | 明细第 i 行（i ∈ 1..3）|
| `{{paymentTerms}}` | 付款条款 |
| `{{partyASigner}}` / `{{partyBSigner}}` | 双方授权代表 |

调用：

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"contractNo":"MOLLY-2026-0429-0001","partyA":"示例科技有限公司","partyB":"Molly 开源团队","signDate":"2026-04-29","amount":"50,000.00"}' \
  http://localhost:8081/doc/word/contract.docx \
  -o contract-rendered.docx
```

或直接运行本地冒烟入口（不启动 Web 容器）：

```bash
mvn -pl molly-document-example -am -DskipTests install
cd molly-document-example && mvn dependency:build-classpath -Dmdep.outputFile=target/cp.txt -DincludeScope=runtime
java -cp "target/classes:$(cat target/cp.txt)" cn.molly.example.document.TemplateSmokeMain
# 输出在 target/smoke/ 下
```

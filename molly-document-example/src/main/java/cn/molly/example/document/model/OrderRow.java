package cn.molly.example.document.model;

import cn.molly.document.excel.annotation.ExcelColumn;
import cn.molly.document.excel.annotation.ExcelDateFormat;
import cn.molly.document.excel.annotation.ExcelEnum;
import cn.molly.document.excel.annotation.ExcelSheet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 示例订单实体，演示注解驱动导入导出。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ExcelSheet(name = "订单")
public class OrderRow {

    @ExcelColumn(header = "订单号", order = 1, width = 24)
    private String orderNo;

    @ExcelColumn(header = "客户.姓名", order = 2, width = 16)
    private String customerName;

    @ExcelColumn(header = "客户.手机", order = 3, width = 16)
    private String customerPhone;

    @ExcelColumn(header = "金额", order = 4, width = 14)
    private BigDecimal amount;

    @ExcelColumn(header = "状态", order = 5, width = 12)
    @ExcelEnum(labelMethod = "getLabel", codeMethod = "getCode")
    private OrderStatus status;

    @ExcelColumn(header = "创建时间", order = 6, width = 22)
    @ExcelDateFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 订单状态枚举。
     */
    public enum OrderStatus {
        CREATED("C", "待支付"),
        PAID("P", "已支付"),
        SHIPPED("S", "已发货"),
        CANCELLED("X", "已取消");

        private final String code;
        private final String label;

        OrderStatus(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }
}

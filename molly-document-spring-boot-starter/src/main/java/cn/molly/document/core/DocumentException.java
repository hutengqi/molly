package cn.molly.document.core;

/**
 * 文档组件统一运行时异常。
 * <p>
 * 所有子能力（Word / Excel / PDF / Email）的内部 IO 或模板渲染异常均包装为此类型抛出，
 * 便于使用方在上层统一捕获。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public class DocumentException extends RuntimeException {

    public DocumentException(String message) {
        super(message);
    }

    public DocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}

package cn.molly.oss.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传进度模型。
 * <p>
 * 记录文件上传过程中的字节传输量与完成百分比，
 * 通过 {@link UploadProgressListener} 回调传递给调用方。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadProgress {

    /**
     * 文件总字节数。若未知则为 -1。
     */
    private long totalBytes;

    /**
     * 已传输的字节数。
     */
    private long transferredBytes;

    /**
     * 上传完成百分比（0-100）。若总大小未知则为 -1。
     */
    private int percentage;

    /**
     * 根据已传输和总字节数创建进度实例，自动计算百分比。
     *
     * @param totalBytes       总字节数
     * @param transferredBytes 已传输字节数
     * @return 进度实例
     */
    public static UploadProgress of(long totalBytes, long transferredBytes) {
        int pct = totalBytes > 0 ? (int) (transferredBytes * 100 / totalBytes) : -1;
        return new UploadProgress(totalBytes, transferredBytes, pct);
    }
}

package cn.molly.oss.core;

/**
 * 上传进度回调监听器。
 * <p>
 * 函数式接口，在文件上传过程中被周期性调用，
 * 将当前的 {@link UploadProgress} 传递给调用方，用于实现进度条等功能。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/23
 */
@FunctionalInterface
public interface UploadProgressListener {

    /**
     * 上传进度变更时回调。
     *
     * @param progress 当前上传进度
     */
    void onProgress(UploadProgress progress);
}

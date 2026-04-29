package cn.molly.document.word;

import com.deepoove.poi.data.PictureRenderData;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.data.TextRenderData;

/**
 * poi-tl 渲染模型辅助构造工具。
 * <p>
 * 封装常用的图片、文本模型构造，屏蔽 poi-tl API 变化对上层业务的影响。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
public final class WordRenderModel {

    private WordRenderModel() {
    }

    /**
     * 构造文本片段模型。
     *
     * @param text 文本
     * @return poi-tl 文本渲染数据
     */
    public static TextRenderData text(String text) {
        return new TextRenderData(text);
    }

    /**
     * 构造图片渲染模型（按字节）。
     *
     * @param bytes  图片字节
     * @param width  像素宽度
     * @param height 像素高度
     * @return poi-tl 图片渲染数据
     */
    public static PictureRenderData picture(byte[] bytes, int width, int height) {
        return Pictures.ofBytes(bytes).size(width, height).create();
    }

    /**
     * 构造图片渲染模型（按路径）。
     *
     * @param path   图片路径 (本地 / URL)
     * @param width  像素宽度
     * @param height 像素高度
     * @return poi-tl 图片渲染数据
     */
    public static PictureRenderData picture(String path, int width, int height) {
        return Pictures.of(path).size(width, height).create();
    }
}

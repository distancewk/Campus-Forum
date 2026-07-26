package com.campus.common.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * 后端 HTML 清洗（信任边界修复）。
 * 所有来自客户端的内容在入库前必须经过清洗，避免存储型 XSS / HTML 注入。
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    /**
     * 纯文本字段（标题、昵称、简介等）：剥离所有标签。
     */
    public static String cleanPlain(String s) {
        if (s == null) {
            return null;
        }
        return Jsoup.clean(s, Safelist.none());
    }

    /**
     * 富文本字段（帖子正文等）：仅保留基础安全的格式化标签，
     * 在 Safelist.basic() 基础上显式允许 https 图片，便于保留现有富文本/图片功能。
     */
    public static String cleanBasic(String s) {
        if (s == null) {
            return null;
        }
        Safelist whitelist = Safelist.basic()
                .addTags("img")
                .addAttributes("img", "src", "alt", "width", "height")
                .addProtocols("img", "src", "http", "https");
        return Jsoup.clean(s, whitelist);
    }
}

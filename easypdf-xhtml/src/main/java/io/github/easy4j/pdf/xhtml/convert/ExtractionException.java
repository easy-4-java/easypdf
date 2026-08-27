package io.github.easy4j.pdf.xhtml.convert;

import java.io.IOException;

/**
 * PDF 提取失败的分类异常：携带 {@link Code} 判别失败原因（损坏 / 加密 / 超限 / 不存在），
 * 供智能体与服务端按类处置而非依赖消息文案。
 *
 * <p>继承 {@link IOException}——既有按 IOException 捕获的调用方无需改动；
 * 消息保持人类可读，机器判定一律走 {@link #getCode()}。
 */
public class ExtractionException extends IOException {

    private static final long serialVersionUID = 1L;

    /** 失败分级。 */
    public enum Code {
        /** 文件损坏或不可解析（含非 PDF 字节流）。 */
        CORRUPT,
        /** 受密码保护 / 已加密且未提供可用口令。 */
        ENCRYPTED,
        /** 超出护栏上限（文件大小 maxFileBytes 或页数 maxPages）。 */
        LIMIT_EXCEEDED,
        /** 目标文件不存在。 */
        NOT_FOUND
    }

    private final Code code;

    public ExtractionException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public ExtractionException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** 失败分级码。 */
    public Code getCode() {
        return code;
    }
}

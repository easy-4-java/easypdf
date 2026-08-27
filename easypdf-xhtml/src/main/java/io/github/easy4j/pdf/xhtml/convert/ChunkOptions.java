package io.github.easy4j.pdf.xhtml.convert;

/**
 * 切片配置：按字符数切分正文，相邻 chunk 重叠 {@code overlapChars} 字符以保留上下文。
 * 与 {@link DocumentChunker#chunk(DocumentStructure, ChunkOptions)} 配套使用。
 */
public final class ChunkOptions {

    /** 单片最大字符数（默认 800：常见 LLM 窗口下留有安全余量的经验值）。 */
    public int maxChars = 800;

    /** 相邻 chunk 重叠字符数（默认 100），实际取 min(overlapChars, maxChars - 1)。 */
    public int overlapChars = 100;

    /** chunk 来源标识（建议传 PDF 文件名），作为 id 前缀与 source；null 时使用 "doc"。 */
    public String idPrefix;
}

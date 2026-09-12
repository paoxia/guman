package io.github.paoxia.guman.domain.chat;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/** 表示一次聊天消息携带的、已通过大小和类型校验的附件。 */
public class ChatAttachment {

    /** 单个附件允许的最大字节数。 */
    public static final int MAX_SIZE_BYTES = 5 * 1024 * 1024;

    private static final Set<String> IMAGE_MEDIA_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp");

    private static final Set<String> TEXT_APPLICATION_MEDIA_TYPES =
            Set.of(
                    "application/json",
                    "application/pdf",
                    "application/xml",
                    "application/yaml",
                    "application/x-yaml",
                    "application/javascript");

    /*
     * 仅用于向用户和模型标识附件，不作为文件系统路径使用。
     */
    private final String name;

    /*
     * 经过规范化且受支持的 MIME 类型。
     */
    private final String mediaType;

    /*
     * 附件原始内容，对外访问时始终返回副本。
     */
    private final byte[] content;

    /**
     * 创建经过领域约束校验的聊天附件。
     *
     * @param name 附件显示名称
     * @param mediaType 附件 MIME 类型
     * @param content 附件原始内容
     */
    public ChatAttachment(String name, String mediaType, byte[] content) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException("attachment name must contain 1 to 255 characters");
        }
        String normalizedMediaType =
                mediaType == null ? "" : mediaType.strip().toLowerCase(Locale.ROOT);
        if (!isSupportedMediaType(normalizedMediaType)) {
            throw new IllegalArgumentException("unsupported attachment media type");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("attachment must not be empty");
        }
        if (content.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("attachment exceeds the 5 MB size limit");
        }
        this.name = name.strip();
        this.mediaType = normalizedMediaType;
        this.content = Arrays.copyOf(content, content.length);
    }

    /**
     * 判断 MIME 类型是否可作为图片或文本附件处理。
     *
     * @param mediaType 待检查的规范化 MIME 类型
     * @return 支持时返回 true
     */
    public static boolean isSupportedMediaType(String mediaType) {
        return mediaType != null
                && (IMAGE_MEDIA_TYPES.contains(mediaType)
                        || mediaType.startsWith("text/")
                        || TEXT_APPLICATION_MEDIA_TYPES.contains(mediaType));
    }

    /**
     * 获取附件显示名称。
     *
     * @return 附件显示名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取附件 MIME 类型。
     *
     * @return 规范化的 MIME 类型
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * 获取附件内容的防御性副本。
     *
     * @return 附件原始字节副本
     */
    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

    /**
     * 判断当前附件是否为模型可识别的图片。
     *
     * @return 图片附件返回 true
     */
    public boolean isImage() {
        return IMAGE_MEDIA_TYPES.contains(mediaType);
    }

    /**
     * 判断当前附件是否为 PDF 文档。
     *
     * @return PDF 附件返回 true
     */
    public boolean isPdf() {
        return "application/pdf".equals(mediaType);
    }
}

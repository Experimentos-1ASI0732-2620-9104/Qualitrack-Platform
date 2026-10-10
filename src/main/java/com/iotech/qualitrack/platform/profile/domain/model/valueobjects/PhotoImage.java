package com.iotech.qualitrack.platform.profile.domain.model.valueobjects;

import java.util.Arrays;

/**
 * Image file of a profile photo. The type is detected from the content, not from the name or the header sent by the
 * client, so only real JPEG, PNG or WebP images are accepted.
 *
 * @param content bytes of the image, at most {@link #MAX_SIZE_BYTES}
 * @param contentType media type detected from the content
 */
public record PhotoImage(byte[] content, String contentType) {
    public static final int MAX_SIZE_BYTES = 2 * 1024 * 1024;

    public PhotoImage {
        if (content == null || content.length == 0) throw new IllegalArgumentException("The photo is empty");
        if (content.length > MAX_SIZE_BYTES) throw new IllegalArgumentException("The photo cannot exceed 2 MB");
        var detected = detectContentType(content);
        if (detected == null) throw new IllegalArgumentException("The photo must be a JPEG, PNG or WebP image");
        if (contentType != null && !contentType.equals(detected)) {
            throw new IllegalArgumentException("The content type does not match the image");
        }
        content = content.clone();
        contentType = detected;
    }

    /**
     * @param content bytes received from the client
     * @return the image with the type detected from its content
     * @throws IllegalArgumentException when it is empty, too large or not a JPEG, PNG or WebP image
     */
    public static PhotoImage of(byte[] content) {
        return new PhotoImage(content, null);
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public int size() {
        return content.length;
    }

    private static String detectContentType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return "image/png";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PhotoImage image && contentType.equals(image.contentType)
                && Arrays.equals(content, image.content);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(content) + contentType.hashCode();
    }

    @Override
    public String toString() {
        return "PhotoImage[contentType=" + contentType + ", size=" + content.length + "]";
    }
}

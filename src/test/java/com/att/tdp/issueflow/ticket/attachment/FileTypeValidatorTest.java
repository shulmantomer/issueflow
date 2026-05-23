package com.att.tdp.issueflow.ticket.attachment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FileTypeValidatorTest {

    private static final byte[] PNG =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
    private static final byte[] PDF = {0x25, 0x50, 0x44, 0x46, 1, 2, 3};

    @Test
    void acceptsValidPng() {
        assertThatCode(() -> FileTypeValidator.validate("image/png", PNG))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsValidJpegAndPdf() {
        assertThatCode(() -> FileTypeValidator.validate("image/jpeg", JPEG))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileTypeValidator.validate("application/pdf", PDF))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsPlainTextUtf8() {
        assertThatCode(() -> FileTypeValidator.validate("text/plain",
                "hello world".getBytes(StandardCharsets.UTF_8)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMismatchedMagicBytes() {
        assertThatThrownBy(() -> FileTypeValidator.validate("image/png",
                "definitely not a png".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsDisallowedContentType() {
        assertThatThrownBy(() -> FileTypeValidator.validate("application/zip", PDF))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsNullContentType() {
        assertThatThrownBy(() -> FileTypeValidator.validate(null, PNG))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsPlainTextWithNullByte() {
        assertThatThrownBy(() -> FileTypeValidator.validate("text/plain",
                new byte[]{72, 0, 73}))
                .isInstanceOf(BadRequestException.class);
    }
}

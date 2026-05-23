package com.att.tdp.issueflow.ticket.attachment;

import com.att.tdp.issueflow.exception.BadRequestException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class FileTypeValidator {

    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PDF_SIGNATURE = {0x25, 0x50, 0x44, 0x46}; // %PDF

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/png", "image/jpeg", "application/pdf", "text/plain");

    private FileTypeValidator() {
    }

    public static void validate(String declaredContentType, byte[] data) {
        if (declaredContentType == null || !ALLOWED_TYPES.contains(declaredContentType)) {
            throw new BadRequestException("Unsupported content type '" + declaredContentType
                    + "'. Allowed: image/png, image/jpeg, application/pdf, text/plain");
        }
        boolean matches = switch (declaredContentType) {
            case "image/png" -> startsWith(data, PNG_SIGNATURE);
            case "image/jpeg" -> startsWith(data, JPEG_SIGNATURE);
            case "application/pdf" -> startsWith(data, PDF_SIGNATURE);
            case "text/plain" -> isPlainText(data);
            default -> false;
        };
        if (!matches) {
            throw new BadRequestException("File content does not match the declared type '"
                    + declaredContentType + "'");
        }
    }

    private static boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPlainText(byte[] data) {
        for (byte b : data) {
            if (b == 0) {
                return false; // null byte signals binary content
            }
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}

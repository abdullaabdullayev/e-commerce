package com.example.courseservice.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
import static com.example.courseservice.exception.DomainException.badRequest;

@Component
public class MaterialValidator {
    public record FileInfo(String filename, String contentType) {}

    public FileInfo validate(MultipartFile file) throws IOException {
        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (name.isBlank() || name.length() > 200 || name.chars().anyMatch(Character::isISOControl)
                || file.isEmpty() || file.getSize() > 26214400) {
            throw badRequest("Invalid filename or file size (maximum 25 MB)");
        }
        return new FileInfo(name, contentType(file, name.toLowerCase(Locale.ROOT)));
    }

    public void validateLink(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw badRequest("Use an HTTPS link without embedded credentials");
        }
    }

    private String contentType(MultipartFile file, String name) throws IOException {
        byte[] head;
        try (InputStream input = file.getInputStream()) {
            head = input.readNBytes(8);
        }
        if (name.endsWith(".pdf") && new String(head, StandardCharsets.US_ASCII).startsWith("%PDF-")) return "application/pdf";
        if (name.endsWith(".png") && Arrays.equals(head, new byte[]{(byte)137, 80, 78, 71, 13, 10, 26, 10})) return "image/png";
        if ((name.endsWith(".jpg") || name.endsWith(".jpeg")) && head.length >= 3
                && head[0] == (byte)255 && head[1] == (byte)216 && head[2] == (byte)255) return "image/jpeg";
        if (name.endsWith(".ino") && file.getSize() <= 1048576) {
            try {
                String content = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(file.getBytes())).toString();
                if (content.indexOf(0) >= 0) throw badRequest("Arduino files must be UTF-8 text");
            } catch (CharacterCodingException exception) {
                throw badRequest("Arduino files must be UTF-8 text");
            }
            return "text/plain";
        }
        if (name.endsWith(".pptx") && isPresentation(file)) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        throw badRequest("Allowed files: PDF, PNG, JPEG, PPTX and UTF-8 INO; content must match the extension");
    }

    private boolean isPresentation(MultipartFile file) throws IOException {
        boolean types = false;
        boolean presentation = false;
        int count = 0;
        long expanded = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > 2000) throw badRequest("Presentation contains too many entries");
                types |= entry.getName().equals("[Content_Types].xml");
                presentation |= entry.getName().equals("ppt/presentation.xml");
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    expanded += read;
                    if (expanded > 104857600) throw badRequest("Expanded presentation exceeds 100 MB");
                }
            }
        } catch (ZipException exception) {
            throw badRequest("Invalid presentation");
        }
        return types && presentation;
    }
}

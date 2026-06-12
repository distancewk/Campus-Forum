package com.campus.common.util;

import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class FileUtil {

    private static final Map<String, List<String>> ALLOWED_EXTENSIONS = Map.of(
            "image/jpeg", List.of("jpg", "jpeg"),
            "image/png", List.of("png"),
            "image/gif", List.of("gif"),
            "image/webp", List.of("webp")
    );

    @Value("${campus.upload.path:./uploads/}")
    private String uploadPath;

    @Value("${campus.upload.allowed-types:image/jpeg,image/png,image/gif,image/webp}")
    private String allowedTypes;

    @Value("${campus.upload.max-size:5242880}")
    private long maxSize;

    /**
     * Upload a file and return the relative path.
     *
     * @param file   the multipart file
     * @param subDir subdirectory (e.g., "avatar/", "post/")
     * @return relative path like /uploads/avatar/2026/05/28/uuid.jpg
     */
    public String upload(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }

        // Validate file type
        String contentType = file.getContentType();
        List<String> allowed = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .toList();
        if (contentType == null || !allowed.contains(contentType)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }

        // Validate file size
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        }

        // Generate UUID filename
        String ext = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.getOrDefault(contentType, List.of()).contains(ext)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        if (!hasValidSignature(file, contentType)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }
        String fileName = UUID.randomUUID() + "." + ext;

        // Generate date-based subdirectory
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = subDir + datePath + "/" + fileName;
        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(relativePath).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传路径不合法");
        }

        // Create directories if not exist
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建上传目录失败");
        }

        // Save file
        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传失败");
        }

        return "/uploads/" + relativePath;
    }

    private boolean hasValidSignature(MultipartFile file, String contentType) {
        byte[] header = new byte[12];
        int length;
        try (InputStream inputStream = file.getInputStream()) {
            length = inputStream.read(header);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件读取失败");
        }
        if (length < 4) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg" -> isJpeg(header);
            case "image/png" -> isPng(header);
            case "image/gif" -> isGif(header);
            case "image/webp" -> isWebp(header, length);
            default -> false;
        };
    }

    private boolean isJpeg(byte[] header) {
        return (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        return (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private boolean isGif(byte[] header) {
        return header[0] == 0x47
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x38
                && (header[4] == 0x37 || header[4] == 0x39)
                && header[5] == 0x61;
    }

    private boolean isWebp(byte[] header, int length) {
        return length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50;
    }
}

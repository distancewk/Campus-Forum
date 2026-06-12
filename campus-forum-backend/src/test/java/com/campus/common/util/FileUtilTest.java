package com.campus.common.util;

import com.campus.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilTest {

    @TempDir
    private Path uploadDir;

    private FileUtil fileUtil;

    @BeforeEach
    void setUp() {
        fileUtil = new FileUtil();
        ReflectionTestUtils.setField(fileUtil, "uploadPath", uploadDir.toString() + "/");
        ReflectionTestUtils.setField(fileUtil, "allowedTypes", "image/jpeg,image/png,image/gif,image/webp");
        ReflectionTestUtils.setField(fileUtil, "maxSize", 5242880L);
    }

    @Test
    void uploadAcceptsPngWithValidSignature() {
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);

        String path = fileUtil.upload(file, "avatar/");

        assertThat(path).startsWith("/uploads/avatar/");
        assertThat(path).endsWith(".png");
    }

    @Test
    void uploadRejectsSpoofedImageContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "not an image".getBytes()
        );

        assertThatThrownBy(() -> fileUtil.upload(file, "avatar/"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件格式");
    }
}

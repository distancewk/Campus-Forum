package com.campus.ai.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class DocumentTextExtractor {

    public String extract(MultipartFile file) {
        try {
            String extension = extensionOf(file);
            if (extension.equals(".txt") || extension.equals(".md") || extension.equals(".markdown")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            if (extension.equals(".pdf")) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    return new PDFTextStripper().getText(document);
                }
            }
            if (extension.equals(".docx")) {
                try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
                    return document.getParagraphs().stream()
                            .map(XWPFParagraph::getText)
                            .collect(Collectors.joining("\n"));
                }
            }
            throw new IllegalArgumentException("不支持的资料格式");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("资料文本抽取失败", e);
        }
    }

    private String extensionOf(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) {
            return "";
        }
        String filename = file.getOriginalFilename().toLowerCase(Locale.ROOT);
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex);
    }
}

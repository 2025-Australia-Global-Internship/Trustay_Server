package com.maritel.trustay.service;

import com.maritel.trustay.util.FileUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class FileService {

    @Value("${pkg.imgLocation}")
    private String imageStoragePath;

    @Value("${pkg.server.domain}")
    private String serverDomain;

    @Value("${pkg.file.prefix}")
    private String filePrefix;

    public String uploadFile(MultipartFile file) throws MalformedURLException, BadRequestException {
        if (file == null) {
            throw new BadRequestException("파일이 제공되지 않았습니다.");
        }

        String fileExt = resolveImageExtension(file);

        log.debug("** fileUpload originalFileName = {} **", file.getOriginalFilename());

        if (fileExt.equals(".jpg") || fileExt.equals(".jpeg") || fileExt.equals(".png") || fileExt.equals(".heic") || fileExt.equals(".heif")) {
            String fileName = this.getNewFileName(fileExt);
            String url = this.uploadLocal(fileName, file);
            log.info(">>>>  fileUpload fileurl {}**", url);
            return url;
        } else {
            log.warn("지원하지 않는 파일 형식: {}", fileExt);
            return null;
        }
    }

    public List<String> uploadFiles(List<MultipartFile> files) throws MalformedURLException, BadRequestException {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String url = uploadFile(file);
            if (url != null && !url.isBlank()) {
                urls.add(url);
            }
        }
        return urls;
    }

    private String uploadLocal(String filename, MultipartFile file) throws MalformedURLException {
        LocalDate nowDate = LocalDate.now();
        String datePath = String.format("%04d/%02d/%02d",
                nowDate.getYear(),
                nowDate.getMonth().getValue(),
                nowDate.getDayOfMonth());

        String fullPath = imageStoragePath + "/" + datePath;
        FileUtils.hasDirectoryAndMkDir(fullPath);

        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                boolean mkdir = dir.mkdirs();
                log.debug("디렉토리 생성 성공: {}", mkdir);
            }

            FileOutputStream fileOutputStream = new FileOutputStream(new File(dir, filename));
            BufferedOutputStream stream = new BufferedOutputStream(fileOutputStream);
            stream.write(file.getBytes());
            stream.close();

        } catch (IOException e) {
            log.error("파일 업로드 중 오류 발생", e);
            throw new RuntimeException("파일 업로드 실패: " + e.getLocalizedMessage());
        }

        // URL 생성 (File.separator 대신 "/" 사용)
        String uploadedURLPath = String.format("http://%s:8080/images/%s/%s",
                serverDomain, datePath, filename);
        return uploadedURLPath;
    }

    /**
     * 계약서 스캔 원본 이미지 저장. URL 경로: /images/contracts/scans/…
     */
    public String uploadContractScanImage(MultipartFile file) throws MalformedURLException, BadRequestException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("파일이 제공되지 않았습니다.");
        }
        String fileExt = resolveImageExtension(file);
        if (!(fileExt.equals(".jpg") || fileExt.equals(".jpeg") || fileExt.equals(".png")
                || fileExt.equals(".heic") || fileExt.equals(".heif"))) {
            log.warn("계약 스캔에 지원하지 않는 형식: {}", fileExt);
            throw new BadRequestException("지원하지 않는 이미지 형식입니다.");
        }
        String fileName = getNewFileName(fileExt);
        return uploadLocalUnderPrefix("contracts/scans", fileName, file);
    }

    /**
     * OCR 결과 PDF 저장. URL 경로: /images/contracts/pdf/…
     */
    public String saveContractPdf(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF 데이터가 비어 있습니다.");
        }
        LocalDate nowDate = LocalDate.now();
        String datePath = String.format("%04d/%02d/%02d",
                nowDate.getYear(),
                nowDate.getMonth().getValue(),
                nowDate.getDayOfMonth());
        String relativeDir = "contracts/pdf/" + datePath;
        String fullPath = imageStoragePath + "/" + relativeDir.replace("/", java.io.File.separator);
        FileUtils.hasDirectoryAndMkDir(fullPath);

        String fileName = getNewFileName(".pdf");
        java.io.File outFile = new java.io.File(fullPath, fileName);
        try {
            Files.write(outFile.toPath(), pdfBytes);
        } catch (IOException e) {
            log.error("PDF 저장 실패", e);
            throw new RuntimeException("PDF 저장 실패: " + e.getLocalizedMessage());
        }
        return String.format("http://%s:8080/images/%s/%s", serverDomain, relativeDir.replace("\\", "/"), fileName);
    }

    private String uploadLocalUnderPrefix(String pathPrefix, String filename, MultipartFile file)
            throws MalformedURLException, BadRequestException {
        LocalDate nowDate = LocalDate.now();
        String datePath = String.format("%04d/%02d/%02d",
                nowDate.getYear(),
                nowDate.getMonth().getValue(),
                nowDate.getDayOfMonth());
        String relativeDir = pathPrefix + "/" + datePath;
        String fullPath = imageStoragePath + "/" + relativeDir.replace("/", java.io.File.separator);
        FileUtils.hasDirectoryAndMkDir(fullPath);

        try {
            java.io.File dir = new java.io.File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File(dir, filename));
                 java.io.BufferedOutputStream stream = new java.io.BufferedOutputStream(fos)) {
                stream.write(file.getBytes());
            }
        } catch (IOException e) {
            log.error("파일 업로드 중 오류", e);
            throw new RuntimeException("파일 업로드 실패: " + e.getLocalizedMessage());
        }

        return String.format("http://%s:8080/images/%s/%s",
                serverDomain, relativeDir.replace("\\", "/"), filename);
    }

    /* 새 파일명 규칙 적용 */
    public String getNewFileName(String fileExt) {
        String randomStr = UUID.randomUUID().toString().replace("-", "");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String formatTime = LocalDateTime.now().format(dateTimeFormatter);

        String newFileName = String.format("%s_%s_%s%s",
                filePrefix, randomStr, formatTime, fileExt);

        log.debug("생성된 파일명: {}", newFileName);
        return newFileName;
    }

    /**
     * 안전한 이미지 확장자 해석:
     * 1) 먼저 contentType → extension(FileUtils.extension) 시도
     * 2) FileUtils.extension이 ".bin"을 반환하거나 예외가 발생하면 업로드된 originalFilename의 확장자(소문자)를 사용
     * 3) 그래도 확장자를 찾지 못하면 ".bin"을 반환
     */
    private String resolveImageExtension(MultipartFile file) {
        if (file == null) {
            return ".bin";
        }

        String contentType = file.getContentType();
        try {
            if (contentType != null && !contentType.isBlank()) {
                String ext = FileUtils.extension(contentType);
                if (ext != null && !ext.isBlank() && !ext.equalsIgnoreCase(".bin")) {
                    return ext.toLowerCase();
                }
            }
        } catch (Exception e) {
            // content-type으로부터 확장자 결정에 실패한 경우 원본 파일명에서 추출하도록 로그만 남김
            log.debug("contentType -> extension 변환 실패 또는 알 수 없는 MIME 타입 (fallback to filename ext): {}", e.getMessage());
        }

        // original filename에서 확장자 추출
        String original = file.getOriginalFilename();
        if (original != null) {
            int idx = original.lastIndexOf('.');
            if (idx >= 0 && idx < original.length() - 1) {
                String ext = original.substring(idx).toLowerCase();
                return ext;
            }
        }

        // 최후의 수단
        return ".bin";
    }
}
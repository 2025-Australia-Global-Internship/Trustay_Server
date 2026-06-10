package com.maritel.trustay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maritel.trustay.constant.PaperContractScanStatus;
import com.maritel.trustay.dto.res.ChatMessageRes;
import com.maritel.trustay.dto.res.PaperContractDocumentRes;
import com.maritel.trustay.dto.res.PaperContractScanRes;
import com.maritel.trustay.entity.Member;
import com.maritel.trustay.entity.ChatRoom;
import com.maritel.trustay.entity.PaperContractDocument;
import com.maritel.trustay.repository.ChatRoomRepository;
import com.maritel.trustay.repository.MemberRepository;
import com.maritel.trustay.repository.PaperContractDocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.coyote.BadRequestException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaperContractService {

    private final PaperContractDocumentRepository paperContractDocumentRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final FileService fileService;
    private final TesseractOcrService tesseractOcrService;
    private final ContractScanPdfService contractScanPdfService;
    private final ChatMessageService chatMessageService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 스캔 이미지 업로드 → Tesseract OCR → 페이지별 이미지가 담긴 PDF 생성 → 저장 후 채팅 CONTRACT 메시지 브로드캐스트
     */
    public PaperContractScanRes scanAndStore(Long roomId, Long memberId, List<MultipartFile> images)
            throws BadRequestException {
        if (CollectionUtils.isEmpty(images)) {
            throw new BadRequestException("Please upload at least one image.");
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        if (!room.getSender().getId().equals(memberId) && !room.getReceiver().getId().equals(memberId)) {
            throw new IllegalArgumentException("You are not a participant in this chat room.");
        }

        List<BufferedImage> bufferedImages = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            BufferedImage bi;
            try {
                bi = ImageIO.read(file.getInputStream());
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't read image: " + file.getOriginalFilename(), e);
            }
            if (bi == null) {
                throw new BadRequestException("One of the files isn't a readable image: " + file.getOriginalFilename());
            }
            bufferedImages.add(bi);

            // [수정됨] MalformedURLException 예외 처리 추가
            try {
                imageUrls.add(fileService.uploadContractScanImage(file));
            } catch (MalformedURLException e) {
                log.error("URL 생성 오류", e);
                throw new IllegalStateException("Failed to build the file URL during upload: " + file.getOriginalFilename(), e);
            }
        }

        if (bufferedImages.isEmpty()) {
            throw new BadRequestException("No valid images were provided.");
        }

        String ocrText;
        try {
            ocrText = tesseractOcrService.recognizeAll(bufferedImages);
        } catch (TesseractException e) {
            log.error("Tesseract OCR 실패", e);
            throw new IllegalStateException("OCR failed. Please check your Tesseract install, pkg.ocr.tesseract-data-path, and language data (kor, eng).", e);
        }

        byte[] pdfBytes;
        try {
            pdfBytes = contractScanPdfService.buildPdfFromImages(bufferedImages);
        } catch (IOException e) {
            log.error("PDF 생성 실패", e);
            throw new IllegalStateException("Failed to generate the PDF.", e);
        }

        String pdfUrl = fileService.saveContractPdf(pdfBytes);

        String urlsJson;
        try {
            urlsJson = objectMapper.writeValueAsString(imageUrls);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metadata.", e);
        }

        PaperContractDocument doc = PaperContractDocument.builder()
                .chatRoom(room)
                .sharehouse(room.getSharehouse())
                .uploadedBy(member)
                .sourceImageUrlsJson(urlsJson)
                .pdfUrl(pdfUrl)
                .ocrText(ocrText)
                .status(PaperContractScanStatus.COMPLETED)
                .build();
        paperContractDocumentRepository.save(doc);

        ChatMessageRes chatRes = chatMessageService.saveContractScanMessage(roomId, memberId, pdfUrl, doc);
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, chatRes);

        return PaperContractScanRes.builder()
                .paperContractDocumentId(doc.getId())
                .pdfUrl(pdfUrl)
                .ocrText(ocrText)
                .status(doc.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public PaperContractDocumentRes getDocument(Long documentId, Long memberId) {
        PaperContractDocument doc = paperContractDocumentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found."));
        ChatRoom room = doc.getChatRoom();
        if (!room.getSender().getId().equals(memberId) && !room.getReceiver().getId().equals(memberId)) {
            throw new IllegalArgumentException("You don't have permission to view this document.");
        }

        List<String> urls = parseSourceUrls(doc.getSourceImageUrlsJson());

        return PaperContractDocumentRes.builder()
                .id(doc.getId())
                .roomId(room.getId())
                .houseId(room.getSharehouse() != null ? room.getSharehouse().getId() : null)
                .pdfUrl(doc.getPdfUrl())
                .ocrText(doc.getOcrText())
                .sourceImageUrls(urls)
                .status(doc.getStatus())
                .regTime(doc.getRegTime())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PaperContractDocumentRes> getMyDocuments(String memberEmail) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        return paperContractDocumentRepository.findByUploadedBy_IdOrderByRegTimeDesc(member.getId())
                .stream()
                .map(doc -> PaperContractDocumentRes.builder()
                        .id(doc.getId())
                        .roomId(doc.getChatRoom() != null ? doc.getChatRoom().getId() : null)
                        .houseId(doc.getSharehouse() != null ? doc.getSharehouse().getId() : null)
                        .pdfUrl(doc.getPdfUrl())
                        .ocrText(doc.getOcrText())
                        .sourceImageUrls(parseSourceUrls(doc.getSourceImageUrlsJson()))
                        .status(doc.getStatus())
                        .regTime(doc.getRegTime())
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> parseSourceUrls(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("sourceImageUrlsJson 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
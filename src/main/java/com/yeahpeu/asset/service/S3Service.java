package com.yeahpeu.asset.service;

import com.yeahpeu.asset.domain.FileType;
import com.yeahpeu.common.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RequiredArgsConstructor
@Service
public class S3Service {

    private final S3Client s3Client;
    private final Tika tika = new Tika();

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public FileUploadDTO upload(MultipartFile file) throws IOException {
        // 1 MIME 타입 판별 (파일 헤더 검사)
        String mimeType = detectMimeType(file);

        // 2️ 파일 확장자 검증
        String extension = extractExtension(file);
        validateFileType(extension, mimeType);

        // 3 고유한 파일명 생성
        String uniqueFileName = generateUniqueFileName();

        // 4️ S3 업로드
        uploadToS3(file, uniqueFileName, mimeType);

        // 5 업로드된 파일 URL 반환
        String url = s3Client.utilities()
                .getUrl(b -> b.bucket(bucket).key(uniqueFileName))
                .toString();

        return new FileUploadDTO(url, mimeType);
    }

    private String mapTikaMimeType(String mimeType, String filename) {
        if ("application/x-tika-ooxml".equals(mimeType)) {
            if (filename.endsWith(".docx")) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            if (filename.endsWith(".xlsx")) {
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
            if (filename.endsWith(".pptx")) {
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            }
        }

        // 한글 파일 보정
        if ("application/x-tika-msoffice".equals(mimeType) && filename.endsWith(".hwp")) {
            return "application/vnd.hancom.hwp";
        }

        return mimeType;
    }

    private String detectMimeType(MultipartFile file) throws IOException {
        String mimeType;

        // 1. Tika를 사용하여 MIME 타입 감지
        try (InputStream is = file.getInputStream()) {
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler();
            ParseContext context = new ParseContext();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(is, handler, metadata, context);
            mimeType = metadata.get(Metadata.CONTENT_TYPE);
        } catch (TikaException | SAXException e) {
            throw new BadRequestException("지원하지 않는 파일 형식 입니다.");
        }

        // 2️. 특정한 경우 표준 MIME 타입으로 변환
        mimeType = mapTikaMimeType(mimeType, file.getOriginalFilename());

        // 3. ZIP 내부 검사 (DOCX 판별)
        if ("application/zip".equals(mimeType) && isDocxFile(file)) {
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        if ("text/plain".equals(mimeType) && extractExtension(file).equals("csv")) {
            mimeType = "text/csv";
        }

        // 4. Java 기본 기능으로 MIME 확인 (Tika가 잘못 반환하는 경우 대비)
        if (mimeType == null || mimeType.equals("application/octet-stream")) {
            Path tempFile = Files.createTempFile("upload_", "." + extractExtension(file));
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            mimeType = Files.probeContentType(tempFile);
            Files.delete(tempFile);
        }

        return mimeType != null ? mimeType : "application/octet-stream";
    }

    /**
     * ZIP 내부 파일을 검사하여 DOCX 여부 확인
     */
    private boolean isDocxFile(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("word/document.xml")) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }


    private static String extractExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("파일 확장자가 없습니다.");
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    }

    private static void validateFileType(String extension, String mimeType) {
        Optional<FileType> fileTypeOpt = FileType.fromExtension(extension);

        // 지원하지 않는 확장자 예외 처리
        if (fileTypeOpt.isEmpty()) {
            throw new BadRequestException("지원하지 않는 파일 형식입니다: " + extension);
        }

        FileType fileType = fileTypeOpt.get();

        // MIME 타입 검증 (확장자와 실제 MIME이 일치해야 함)
        if (!fileType.getMimeType().equals(mimeType) && !mimeType.startsWith(fileType.getMimeType())) {
            throw new BadRequestException("잘못된 파일 형식입니다. 확장자와 MIME 타입이 일치하지 않습니다.");
        }
    }

    private static String generateUniqueFileName() {
        String timestamp = Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return timestamp + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void uploadToS3(MultipartFile file, String fileName, String mimeType) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(mimeType)
                .contentLength(file.getSize())
                .build();

        try (InputStream fileStream = file.getInputStream()) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(fileStream, file.getSize()));
        }
    }
}

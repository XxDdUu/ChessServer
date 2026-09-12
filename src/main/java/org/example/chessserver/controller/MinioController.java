package org.example.chessserver.controller;

import lombok.RequiredArgsConstructor;
import org.example.chessserver.service.MinioService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping({"/api/minio", "/api/upload"})
@RequiredArgsConstructor
public class MinioController {

    private final MinioService minioService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = minioService.uploadAvatar(file);
        return ResponseEntity.ok(Map.of(
                "url", url,
                "fileUrl", url,
                "avatarUrl", url
        ));
    }

    @GetMapping("/files/{*objectName}")
    public ResponseEntity<InputStreamResource> getFile(@PathVariable String objectName) {
        String cleanObjectName = objectName != null && objectName.startsWith("/") ? objectName.substring(1) : objectName;
        InputStream inputStream = minioService.getFile(cleanObjectName);
        MediaType mediaType = org.springframework.http.MediaTypeFactory.getMediaType(cleanObjectName)
                .orElse(MediaType.IMAGE_JPEG);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new InputStreamResource(inputStream));
    }
}

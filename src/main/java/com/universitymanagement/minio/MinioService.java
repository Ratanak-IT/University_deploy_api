package com.universitymanagement.minio;

import com.universitymanagement.minio.dto.FileStream;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MinioService {
    String uploadAsset(MultipartFile file);
    String uploadLessonFile(MultipartFile file);
    String getPublicUrl(String objectName);
    String getPreviewUrl(String objectName);
    String getDownloadUrl(String objectName, String originalFileName);
    void deleteLessonFile(String objectName);
    void deleteAsset(String objectName);
    List<String> getAllFileByMinio();
    FileStream getLessonObject(String objectName);
}

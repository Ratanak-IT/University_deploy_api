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
    String getAssetPreviewUrl(String objectName);

    /**
     * Same as {@link #getDownloadUrl}, but for a file stored via
     * {@link #uploadAsset} / {@link #uploadAssetBytes} — issued certificates,
     * for one, live in the assets bucket, not the lessons bucket.
     */
    String getAssetDownloadUrl(String objectName, String originalFileName);
    void deleteLessonFile(String objectName);
    void deleteAsset(String objectName);
    List<String> getAllFileByMinio();
    FileStream getLessonObject(String objectName);

    /**
     * The raw bytes of a stored asset.
     *
     * <p>Needed where the server has to work on the file itself rather than
     * hand out a link to it — filling an uploaded certificate design, for one.
     */
    byte[] getAssetBytes(String objectName);

    /**
     * Stores bytes the server produced itself.
     *
     * <p>A composed certificate has no MultipartFile behind it, and writing
     * it to a temp file first only to wrap it would buy nothing.
     *
     * @return the object name to store against the record
     */
    String uploadAssetBytes(byte[] data, String fileName, String contentType);
}

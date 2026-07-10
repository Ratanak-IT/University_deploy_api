package com.universitymanagement.minio.service.impl;


import com.universitymanagement.minio.MinioService;
import com.universitymanagement.minio.dto.FileStream;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {
    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket.assets}")
    private String assetsBucket;

    @Value("${minio.bucket.lessons}")
    private String lessonsBucket;

    private static final int PREVIEW_EXPIRY_SECONDS = 10 * 60;
    private static final int DOWNLOAD_EXPIRY_SECONDS = 5 * 60;


    @Override
    public String uploadAsset(MultipartFile file) {
        return upload(file, assetsBucket);
    }

    @Override
    public String uploadLessonFile(MultipartFile file) {
        return upload(file, lessonsBucket);
    }

    private String upload(MultipartFile file, String bucket) {
        try {
            ensureBucketExists(bucket);

            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1L)
                            .contentType(file.getContentType())
                            .build()
            );
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Upload failed: " + e.getMessage(), e);
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }


    @Override
    public String getPublicUrl(String objectName) {
        return endpoint + "/" + assetsBucket + "/" + objectName;
    }

    @Override
    public String getPreviewUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(lessonsBucket)
                            .object(objectName)
                            .expiry(PREVIEW_EXPIRY_SECONDS)
                            .extraQueryParams(Map.of(
                                    "response-content-disposition", "inline"))
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate preview URL", e);
        }
    }

    @Override
    public String getDownloadUrl(String objectName, String originalFileName) {
        try {
            String safeName = originalFileName != null ? originalFileName : objectName;
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(lessonsBucket)
                            .object(objectName)
                            .expiry(DOWNLOAD_EXPIRY_SECONDS)
                            .extraQueryParams(Map.of(
                                    "response-content-disposition",
                                    "attachment; filename=\"" + safeName + "\""))
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }


    @Override
    public FileStream getLessonObject(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(lessonsBucket)
                            .object(objectName)
                            .build()
            );

            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(lessonsBucket)
                            .object(objectName)
                            .build()
            );

            return new FileStream(stream, stat.contentType(), stat.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + objectName, e);
        }
    }

    @Override
    public void deleteLessonFile(String objectName) {
        delete(objectName, lessonsBucket);
    }

    @Override
    public void deleteAsset(String objectName) {
        delete(objectName, assetsBucket);
    }

    private void delete(String objectName, String bucket) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getAllFileByMinio() {
        List<String> files = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(lessonsBucket)
                        .build()
        );

        for (Result<Item> result : results) {
            try {
                files.add(result.get().objectName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return files;
    }
}
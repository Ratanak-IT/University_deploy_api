package com.universitymanagement.minio.dto;


import java.io.InputStream;

public record FileStream(
        InputStream stream,
        String contentType,
        long size
) {

}

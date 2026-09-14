package com.aoo.bcg.media;

import java.io.InputStream;
import java.util.List;

public interface ObjectStorage {
    record UploadedPart(int number,String etag) {}
    String begin(String key,String contentType);
    String uploadPart(String key,String uploadId,int partNumber,byte[] content);
    void complete(String key,String uploadId,List<UploadedPart> parts);
    void abort(String key,String uploadId);
    InputStream open(String key);
    void delete(String key);
}

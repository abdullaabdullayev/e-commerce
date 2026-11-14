package com.example.courseservice.storage;

import java.io.InputStream;

public interface ObjectStorage {
    void put(String key, InputStream input, long size, String contentType);
    InputStream get(String key);
    void remove(String key) throws Exception;
}

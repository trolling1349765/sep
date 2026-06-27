package fpt.capstone.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeFile(MultipartFile file);

    Resource loadFile(String fileUrl);

    void deleteFile(String fileUrl);

    boolean isValidFile(MultipartFile file);
}
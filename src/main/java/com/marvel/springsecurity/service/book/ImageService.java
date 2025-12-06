package com.marvel.springsecurity.service.book;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    private final Cloudinary cloudinary;

    public ImageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @SuppressWarnings({"rawtupes", "unchecked"})
    public Map<String, Object> uploadImage(MultipartFile image, String folder) throws IOException {
        //Creating a temp file to hold data in disk rather than Memory

        File tempFile = null;
        try{
            //temp file will be look like "upload_408383443_image.png"
            //                                    prefix_randomValue_imageName.png
            tempFile = File.createTempFile("upload_","_"+ image.getOriginalFilename());
            image.transferTo(tempFile);

            String fileHash = generateFileHash(tempFile);

            Map<String, Object> property = ObjectUtils.asMap(
                "public_id", folder + "/" + fileHash,
                    "overwrite", true,
                    "resource_type", "image"
            );

            return cloudinary.uploader().upload(tempFile, property);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Could not generate file hash", e);
        } finally{
            if(tempFile != null && tempFile.exists() && !tempFile.delete()){
            logger.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
            }
        }
    }

    public void deleteImage(String publicId) throws IOException {
        try{
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            logger.error("Failed to delete image with Public_id {}", publicId, e);
            throw e;
        }
    }

    /**
     * Generates an MD5 hash for a given file by reading it in chunks.
     * This is memory-efficient for large files.
     * @param file The file to hash.
     * @return A hexadecimal string representation of the hash.
     */
    private String generateFileHash(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        // Read the file in chunks to avoid loading it all into memory
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192]; // 8KB buffer chunk
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

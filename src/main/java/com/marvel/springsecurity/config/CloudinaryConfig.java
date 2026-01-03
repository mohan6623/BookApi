package com.marvel.springsecurity.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryConfig {

//    @Value("${cloudinary.cloud_name}")
    private String cloudName;

//    @Value("${cloudinary.api_key}")
    private String apiKey;

//    @Value("${cloudinary.api_secret}")
    private String apiSecret;

//    @Bean
//    public Cloudinary cloudinary(){
//        Map<String, String> config = new HashMap<>();
//        config.put("cloud_name", cloudName);
//        config.put("api_key", cloudApiKey);
//        config.put("api_secret", cloudApiSecret);
//        return new Cloudinary(config);
//    }
    @Bean
    public Cloudinary cloudinary(){
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }
}

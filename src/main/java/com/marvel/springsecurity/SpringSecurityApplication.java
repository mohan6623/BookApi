package com.marvel.springsecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@SpringBootApplication
public class SpringSecurityApplication {

    public static void main(String[] args) {
        // Force TLS 1.2 for JDBC SSL handshakes (workaround for AEADBadTagException on some networks)
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
        System.setProperty("https.protocols", "TLSv1.2");
        SpringApplication.run(SpringSecurityApplication.class, args);
    }

}

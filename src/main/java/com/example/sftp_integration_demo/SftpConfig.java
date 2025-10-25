package com.example.sftp_integration_demo;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

@Configuration
public class SftpConfig {

    @Value("${sftp.host}")
    private String host;

    @Value("${sftp.port}")
    private int port;

    @Value("${sftp.user}")
    private String user;

    @Value("${sftp.privateKey}")
    private String privateKeyPath;

    @Value("${sftp.knownHosts}")
    private String knownHostsPath;

    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(host);
        factory.setPort(port);
        factory.setUser(user);
        factory.setPrivateKey(new FileSystemResource(privateKeyPath));

        // IT stoped working after adding known_host verification - with only setAllowUnknownKeys(true) it was ok
//        factory.setKnownHostsResource(new FileSystemResource(knownHostsPath));
        factory.setAllowUnknownKeys(true);
        return factory;
    }
}

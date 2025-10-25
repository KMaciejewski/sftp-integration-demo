package com.example.sftp_integration_demo;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Value("${sftp.private-key}")
    private String privateKeyPath;

    @Value("${sftp.known-hosts}")
    private String knownHostsPath;

    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {

        System.out.println("Using SFTP Host: " + host);
        System.out.println("Using SFTP Port: " + port);
        System.out.println("Using SFTP User: " + user);
        System.out.println("Using SFTP Private Key Path: " + privateKeyPath);
        System.out.println("Using SFTP Known Hosts Path: " + knownHostsPath);

        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(false);
        factory.setHost(host);
        factory.setPort(port);
        factory.setUser(user);
        factory.setPrivateKey(new FileSystemResource(privateKeyPath));

        factory.setKnownHostsResource(new FileSystemResource(knownHostsPath));
        factory.setAllowUnknownKeys(false);

        return factory;
    }
}

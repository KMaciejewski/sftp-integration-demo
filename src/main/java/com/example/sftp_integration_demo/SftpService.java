package com.example.sftp_integration_demo;

import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class SftpService {

    public SftpService(SessionFactory<SftpClient.DirEntry> sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private final SessionFactory<SftpClient.DirEntry> sessionFactory;

    public void uploadFile(String remoteDir, String filename, String content) throws Exception {
        try (var session = sessionFactory.getSession()) {
            // Ensure directory exists
            if (!session.exists(remoteDir)) {
                session.mkdir(remoteDir);
            }

            try (var input = new ByteArrayInputStream(content.getBytes())) {
                session.write(input, remoteDir + "/" + filename);
            }
        }
    }

    public boolean fileExists(String remoteDir, String filename) throws Exception {
        try (Session<SftpClient.DirEntry> session = sessionFactory.getSession()) {
            return session.exists(remoteDir + "/" + filename);
        }
    }
}

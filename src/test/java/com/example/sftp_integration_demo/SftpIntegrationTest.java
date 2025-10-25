package com.example.sftp_integration_demo;


import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class SftpIntegrationTest {

    static File pubKey = new File("src/test/resources/keys/id_rsa.pub");

    @Container
    private static final GenericContainer<?> sftpContainer = new GenericContainer<>("atmoz/sftp:latest")
            .withExposedPorts(22)
            .withEnv("SFTP_USERS", "testuser::::upload")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(pubKey.getAbsolutePath()),
                    "/home/testuser/.ssh/keys/id_rsa.pub"
//                    "/home/testuser/.ssh/authorized_keys"
            );
//            .withCreateContainerCmdModifier(cmd -> cmd.getPortBindings().add(
//                    new PortBinding(Ports.Binding.bindPort(2222), new ExposedPort(22))
//            ));
//            .waitingFor(Wait.forListeningPort())
//            .withFileSystemBind("src/test/resources/hostKeys", "/etc/ssh", BindMode.READ_ONLY);

    @Autowired
    private SftpService sftpService;

    @BeforeAll
    static void setup() {
        System.setProperty("sftp.host", sftpContainer.getHost());
        System.setProperty("sftp.port", sftpContainer.getFirstMappedPort().toString());
        System.setProperty("sftp.user", "testuser");
        System.setProperty("sftp.privateKey", "src/test/resources/keys/id_rsa");
        System.setProperty("sftp.knownHosts", "src/test/resources/knownHosts/known_hosts");
    }

    @Test
    void testUploadAndCheckFile() throws Exception {
        String remoteDir = "/upload";
        String filename = "hello.txt";
        String content = "Hello from Testcontainers!";

        sftpService.uploadFile(remoteDir, filename, content);

        assertThat(sftpService.fileExists(remoteDir, filename)).isTrue();

        ExecResult result = sftpContainer.execInContainer("cat", "/home/testuser/upload/hello.txt");
        System.out.println(result.getStdout());

        ExecResult result2 = sftpContainer.execInContainer("ssh-keyscan", "-t", "rsa", "localhost");
        System.out.println(result2.getStdout()); // Might be empty if not pre-populated
    }
}
package com.example.sftp_integration_demo;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class SftpIntegrationTest {

    private static final String SFTP_USER = "testuser";
    private static final String CLIENT_KEYS_PATH = "src/test/resources/client_keys/";
    private static final String HOST_KEYS_PATH = "src/test/resources/host_keys/";

    private static final File CLIENT_PUBLIC_KEY = new File(CLIENT_KEYS_PATH + "id_rsa.pub");

    @Container
    private static final GenericContainer<?> sftpContainer = new GenericContainer<>("atmoz/sftp:latest")
            .withEnv("SFTP_USERS", SFTP_USER + "::::upload")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(CLIENT_PUBLIC_KEY.getAbsolutePath()), "/home/testuser/.ssh/authorized_keys"
            )
            .withCreateContainerCmdModifier(cmd ->
                    Objects.requireNonNull(cmd.getHostConfig())
                            .withPortBindings(new PortBinding(Ports.Binding.bindPort(2222), new ExposedPort(22)))
            )
            .withCopyFileToContainer(MountableFile.forHostPath(HOST_KEYS_PATH), "/etc/ssh")
            .waitingFor(Wait.forListeningPort());

    @Autowired
    private SftpService sftpService;

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
        System.out.println(result2.getStdout());
    }
}
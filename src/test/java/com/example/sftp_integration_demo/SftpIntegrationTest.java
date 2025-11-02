package com.example.sftp_integration_demo;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = {SftpService.class, SftpConfig.class})
class SftpIntegrationTest {

    private static final String SFTP_USER = "testuser";
    private static final String CLIENT_KEYS_PATH = "src/test/resources/ssh_client_keys/";
    private static final String HOST_KEYS_PATH = "src/test/resources/ssh_server_host_keys/";
    private static final String KNOWN_HOSTS_PATH = "src/test/resources/ssh_known_hosts/known_hosts";

    private static final File CLIENT_PUBLIC_KEY = new File(CLIENT_KEYS_PATH + "id_rsa.pub");

    static {
        SftpSshKeyGenerator.generate();
    }

    @Container
    GenericContainer<?> sftpContainer = new GenericContainer<>("atmoz/sftp:latest")
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

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("sftp.host", () -> "localhost");
        registry.add("sftp.port", () -> 2222);
        registry.add("sftp.user", () -> SFTP_USER);
        registry.add("sftp.private-key-path", () -> CLIENT_KEYS_PATH + "id_rsa");
        registry.add("sftp.ssh-known-hosts", () -> KNOWN_HOSTS_PATH);
    }

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
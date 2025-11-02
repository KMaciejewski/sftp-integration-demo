package com.example.sftp_integration_demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public class SftpSshKeyGenerator {

    private static final Path HOST_KEYS_DIR = Path.of("src/test/resources/ssh_server_host_keys");
    private static final Path CLIENT_KEYS_DIR = Path.of("src/test/resources/ssh_client_keys");
    private static final Path KNOWN_HOSTS_DIR = Path.of("src/test/resources/ssh_known_hosts");
    private static final Path KNOWN_HOSTS_FILE = KNOWN_HOSTS_DIR.resolve("known_hosts");

    public static void generate() {
        try {
            createDirs();

            boolean generated = false;

            if (!Files.exists(HOST_KEYS_DIR.resolve("ssh_host_rsa_key"))) {
                generateRsaHostKey();
                generated = true;
            }

            if (!Files.exists(CLIENT_KEYS_DIR.resolve("id_rsa"))) {
                generateClientRsaKey();
                generated = true;
            }

            if (!Files.exists(KNOWN_HOSTS_FILE)) {
                generateKnownHosts();
                generated = true;
            }

            if (generated) {
                System.out.println("✅ SSH keys generated successfully!");
            } else {
                System.out.println("✅ All SSH keys already exist, skipping generation.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SSH keys", e);
        }
    }

    private static void createDirs() throws IOException {
        if (Files.notExists(CLIENT_KEYS_DIR)) {
            Files.createDirectories(CLIENT_KEYS_DIR);
        }
        if (Files.notExists(HOST_KEYS_DIR)) {
            Files.createDirectories(HOST_KEYS_DIR);
        }
        if (Files.notExists(KNOWN_HOSTS_DIR)) {
            Files.createDirectories(KNOWN_HOSTS_DIR);
        }
    }

    private static void generateRsaHostKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        Path privateKey = HOST_KEYS_DIR.resolve("ssh_host_rsa_key");
        Path publicKey = HOST_KEYS_DIR.resolve("ssh_host_rsa_key.pub");

        writePrivateKey(privateKey, pair.getPrivate());
        writePublicKey(publicKey, (RSAPublicKey) pair.getPublic());

        System.out.println("Generated RSA host key");
    }

    private static void generateClientRsaKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        Path privateKey = CLIENT_KEYS_DIR.resolve("id_rsa");
        Path publicKey = CLIENT_KEYS_DIR.resolve("id_rsa.pub");

        writePrivateKey(privateKey, pair.getPrivate());
        writePublicKey(publicKey, (RSAPublicKey) pair.getPublic());

        System.out.println("Generated client RSA key");
    }

    private static void generateKnownHosts() throws Exception {
        Path hostPublicKey = HOST_KEYS_DIR.resolve("ssh_host_rsa_key.pub");
        if (!Files.exists(hostPublicKey)) {
            throw new IllegalStateException("Host public key not found: " + hostPublicKey);
        }

        String hostKey = Files.readString(hostPublicKey).trim();
        String knownHostEntry = "[localhost]:2222 " + hostKey + System.lineSeparator();

        Files.writeString(KNOWN_HOSTS_FILE, knownHostEntry, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Created known_hosts file");
    }

    private static void writePrivateKey(Path path, PrivateKey privateKey) throws IOException {
        String pem = """
                -----BEGIN PRIVATE KEY-----
                %s
                -----END PRIVATE KEY-----
                """.formatted(Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(privateKey.getEncoded()));

        Files.writeString(path, pem, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        path.toFile().setReadable(true, true);
    }

    private static void writePublicKey(Path path, RSAPublicKey publicKey) throws IOException {
        try {
            byte[] sshEncoded = encodeRsaPublicKeySsh(publicKey);
            String b64 = Base64.getEncoder().encodeToString(sshEncoded);
            String sshFormat = "ssh-rsa" + " " + b64 + "\n";
            Files.writeString(path, sshFormat, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new IOException("Failed to write public key in SSH format", e);
        }
    }

    private static byte[] encodeRsaPublicKeySsh(RSAPublicKey key) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, "ssh-rsa");
        writeBigInt(out, key.getPublicExponent());
        writeBigInt(out, key.getModulus());
        return out.toByteArray();
    }

    private static void writeString(ByteArrayOutputStream out, String str) throws IOException {
        byte[] data = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeInt(out, data.length);
        out.write(data);
    }

    private static void writeBigInt(ByteArrayOutputStream out, java.math.BigInteger value) throws IOException {
        byte[] data = value.toByteArray();
        writeInt(out, data.length);
        out.write(data);
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }
}


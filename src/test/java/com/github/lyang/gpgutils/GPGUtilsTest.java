package com.github.lyang.gpgutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GPGUtilsTest {
  private File tempDir;
  private static final long BYTES = 1024 * 1024 * 100; // 100MB
  private final File KEY = new File(Resources.getResource("private-key.gpg").getPath());
  private long outputLength = 0;
  private File plaintextFile;
  private File encryptedFile;

  @Before
  public void setUp() throws IOException, InterruptedException {
    tempDir = Files.createTempDir();
    outputLength = 0;
    assertEquals(0, GPGUtils.importKey(KEY, "--homedir", tempDir.getAbsolutePath()));
    plaintextFile = new File(tempDir, getClass().getSimpleName() + ".txt");
    encryptedFile = new File(plaintextFile.getPath().concat(".gpg"));
    assertEquals(0, generateRandomBytes(plaintextFile, BYTES));
    assertEquals(BYTES, plaintextFile.length());
  }

  @After
  public void tearDown() throws Exception {
    MoreFiles.deleteRecursively(tempDir.toPath());
  }

  @Test
  public void encryptFile() throws IOException, InterruptedException {
    assertEquals(
        0,
        GPGUtils.encryptFile(
            plaintextFile,
            encryptedFile,
            "--homedir",
            tempDir.getAbsolutePath(),
            "--default-recipient-self"));
    assertTrue(encryptedFile.length() > 0);
  }

  @Test
  public void encryptStream() throws IOException, InterruptedException {
    Consumer<InputStream> consumer =
        stream -> {
          try {
            outputLength = ByteStreams.exhaust(stream);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };
    assertEquals(
        0,
        GPGUtils.encryptStream(
            new FileInputStream(plaintextFile),
            consumer,
            "--homedir",
            tempDir.getAbsolutePath(),
            "--default-recipient-self"));
    assertTrue(outputLength > 0);
  }

  @Test
  public void decryptStream() throws IOException, InterruptedException {
    assertEquals(
        0,
        GPGUtils.encryptFile(
            plaintextFile,
            encryptedFile,
            "--homedir",
            tempDir.getAbsolutePath(),
            "--default-recipient-self"));
    Consumer<InputStream> consumer =
        stream -> {
          try {
            outputLength = ByteStreams.exhaust(stream);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };
    assertEquals(
        0,
        GPGUtils.decryptStream(
            new FileInputStream(encryptedFile), consumer, "--homedir", tempDir.getAbsolutePath()));
    assertTrue(outputLength > 0);
  }

  private int generateRandomBytes(File file, long bytes) throws IOException, InterruptedException {
    String command =
        String.format("base64 /dev/urandom | head -c %d > %s", bytes, file.getAbsolutePath());
    return new ProcessBuilder("/bin/sh", "-c", command)
        .redirectError(Redirect.INHERIT)
        .start()
        .waitFor();
  }
}

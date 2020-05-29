package com.github.lyang.gpgutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ObjectArrays;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class GPGUtilsTest {
  private static final long BYTES = 1024 * 1024 * 50; // 50MB
  private static final File KEY = new File(Resources.getResource("private-key.gpg").getPath());
  private static File encryptedFile;
  private static File plaintextFile;
  private static File tempDir;
  private static String[] decryptionArgs;
  private static String[] encryptionArgs;

  @BeforeClass
  public static void beforeClass() throws IOException, InterruptedException {
    tempDir = Files.createTempDir();
    decryptionArgs = new String[] {"--homedir", tempDir.getAbsolutePath()};
    encryptionArgs = ObjectArrays.concat(decryptionArgs, "--default-recipient-self");
    assertEquals(0, GPGUtils.importKey(KEY, "--homedir", tempDir.getAbsolutePath()));
    plaintextFile = new File(tempDir, GPGUtilsTest.class.getCanonicalName() + ".txt");
    encryptedFile = new File(tempDir, GPGUtilsTest.class.getCanonicalName() + ".txt.gpg");
    assertEquals(
        0,
        new ProcessBuilder("openssl", "rand", "-base64", String.valueOf(BYTES))
            .redirectError(Redirect.INHERIT)
            .redirectOutput(plaintextFile)
            .start()
            .waitFor());
    assertEquals(0, GPGUtils.encryptFile(plaintextFile, encryptedFile, encryptionArgs));
  }

  @AfterClass
  public static void tearDown() throws Exception {
    MoreFiles.deleteRecursively(tempDir.toPath());
  }

  @Test
  public void encryptString() throws IOException, InterruptedException {
    StringBuilder builder = new StringBuilder();
    assertEquals(0, GPGUtils.encryptString("GPGUtils", builder, encryptionArgs));
    assertTrue(builder.toString().startsWith("-----BEGIN PGP MESSAGE-----"));
  }

  @Test
  public void encryptFile() throws IOException, InterruptedException {
    File file = new File(tempDir, "encrypted.gpg");
    assertEquals(0, GPGUtils.encryptFile(plaintextFile, file, encryptionArgs));
    assertTrue(file.length() > 0);
  }

  @Test
  public void encryptStream() throws IOException, InterruptedException {
    AtomicLong outputLength = new AtomicLong();
    Consumer<InputStream> consumer =
        stream -> {
          try {
            outputLength.set(ByteStreams.exhaust(stream));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };
    assertEquals(
        0, GPGUtils.encryptStream(new FileInputStream(plaintextFile), consumer, encryptionArgs));
    assertTrue(outputLength.get() > 0);
  }

  @Test
  public void decryptString() throws IOException, InterruptedException {
    StringBuilder encryptedBuilder = new StringBuilder();
    GPGUtils.encryptString("GPGUtils", encryptedBuilder, encryptionArgs);
    StringBuilder decryptedBuilder = new StringBuilder();
    assertEquals(
        0, GPGUtils.decryptString(encryptedBuilder.toString(), decryptedBuilder, decryptionArgs));
    assertEquals("GPGUtils", decryptedBuilder.toString());
  }

  @Test
  public void decryptFile() throws IOException, InterruptedException {
    File file = new File(tempDir, "decrypted.txt");
    assertEquals(0, GPGUtils.decryptFile(encryptedFile, file, decryptionArgs));
    assertTrue(file.length() > 0);
  }

  @Test
  public void decryptStream() throws IOException, InterruptedException {
    AtomicLong outputLength = new AtomicLong();
    Consumer<InputStream> consumer =
        stream -> {
          try {
            outputLength.set(ByteStreams.exhaust(stream));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };
    assertEquals(
        0, GPGUtils.decryptStream(new FileInputStream(encryptedFile), consumer, decryptionArgs));
    assertTrue(outputLength.get() > 0);
  }
}

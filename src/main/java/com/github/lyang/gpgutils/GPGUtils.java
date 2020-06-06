package com.github.lyang.gpgutils;

import com.google.common.collect.ObjectArrays;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GPGUtils} is a simple wrapper for {@link ProcessBuilder} to make calling gpg easier in
 * java.
 */
public class GPGUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(GPGUtils.class);
  private static final Consumer<InputStream> NOOP = stream -> {};
  private static final String[] DEFAULT_ARGS = new String[] {"gpg", "--batch", "--no-tty"};

  /**
   * Import GPG public or private keys
   *
   * @param key The GPG key to be imported
   * @param options CLI options to be passed to gpg {@link Process}
   * @return Exit code of the gpg {@link Process}
   */
  public static int importKey(File key, String... options)
      throws IOException, InterruptedException {
    return processStream("--import", new FileInputStream(key), NOOP, options);
  }

  /**
   * Encrypt a {@link String}
   *
   * @param input The {@link String} to be encrypted
   * @param builder {@link StringBuilder} that will hold the armored result
   * @param options CLI options to be passed to gpg {@link Process}
   * @return Exit code of the gpg {@link Process}
   */
  public static int encryptString(String input, StringBuilder builder, String... options)
      throws IOException, InterruptedException {
    return encryptStream(toInputStream(input), getWriter(builder), merge(options, "--armor"));
  }

  /**
   * Encrypt a {@link File}
   *
   * @param input The {@link File} to be encrypted
   * @param output The {@link File} to hold the encrypted result
   * @param options CLI options to be passed to gpg {@link Process}
   * @return Exit code of the gpg {@link Process}
   */
  public static int encryptFile(File input, File output, String... options)
      throws IOException, InterruptedException {
    return encryptStream(new FileInputStream(input), getWriter(output), options);
  }

  /**
   * Encrypt an {@link InputStream}
   *
   * @param inputStream The {@link InputStream} to be encrypted
   * @param consumer The {@link Consumer} for the encrypted input stream
   * @param options CLI options to be passed to gpg {@link Process}
   * @return Exit code of the gpg {@link Process}
   */
  public static int encryptStream(
      InputStream inputStream, Consumer<InputStream> consumer, String... options)
      throws IOException, InterruptedException {
    return processStream("--encrypt", inputStream, consumer, options);
  }

  /**
   * Decrypt a {@link String}
   *
   * @param input The {@link String} to be decrypted
   * @param builder The {@link StringBuilder} to hold the decrypted result
   * @param options CLI options to be passed to gpg {@link Process}
   * @return Exit code of the gpg {@link Process}
   */
  public static int decryptString(String input, StringBuilder builder, String... options)
      throws IOException, InterruptedException {
    return decryptStream(toInputStream(input), getWriter(builder), options);
  }

  /**
   * Decrypt a {@link File}
   *
   * @param input The {@link File} to be decrypted
   * @param output The {@link File} to hold the decrypted result
   * @param options CLI options to be passed to gpg {@link Process}
   * @return Exit code of the gpg {@link Process}
   */
  public static int decryptFile(File input, File output, String... options)
      throws IOException, InterruptedException {
    return decryptStream(new FileInputStream(input), getWriter(output), options);
  }

  /**
   * Decrypt an {@link InputStream}
   *
   * @param inputStream The {@link InputStream} to be decrypted
   * @param consumer The {@link Consumer} for the decrypted input stream
   * @param options CLI options to be passed to gpg {@link Process}
   * @return Exit code of the gpg {@link Process}
   */
  public static int decryptStream(
      InputStream inputStream, Consumer<InputStream> consumer, String... options)
      throws IOException, InterruptedException {
    return processStream("--decrypt", inputStream, consumer, options);
  }

  /**
   * Process any gpg command against an {@link InputStream}
   *
   * @param command The gpg command
   * @param inputStream The {@link InputStream} to be processed
   * @param consumer The {@link Consumer} for the processed input stream
   * @param options CLI options to be passed to gpg {@link Process}
   * @return Exit code of the gpg {@link Process}
   */
  public static int processStream(
      String command, InputStream inputStream, Consumer<InputStream> consumer, String... options)
      throws IOException, InterruptedException {
    Process process = getGPGProcess(merge(options, command)).start();
    Thread writer = getWriterThread(process, inputStream);
    writer.start();
    consumer.accept(process.getInputStream());
    writer.join();
    return process.waitFor();
  }

  private static ProcessBuilder getGPGProcess(String... options) {
    String[] args = merge(DEFAULT_ARGS, options);
    return new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.INHERIT);
  }

  private static Thread getWriterThread(Process process, InputStream inputStream) {
    return new Thread(
        () -> {
          try (OutputStream stream = process.getOutputStream()) {
            ByteStreams.copy(inputStream, stream);
          } catch (IOException e) {
            LOGGER.error("Failed in writer thread", e);
            throw new RuntimeException(e);
          }
        });
  }

  private static Consumer<InputStream> getWriter(File output) {
    return stream -> {
      try {
        Files.touch(output);
        Files.asByteSink(output).writeFrom(stream);
      } catch (IOException e) {
        LOGGER.error("Failed to write to {}", output.getAbsolutePath(), e);
        throw new RuntimeException(e);
      }
    };
  }

  private static Consumer<InputStream> getWriter(StringBuilder builder) {
    return stream -> {
      try (Reader reader = new InputStreamReader(stream)) {
        builder.append(CharStreams.toString(reader));
      } catch (IOException e) {
        LOGGER.error("Failed to encrypt string", e);
        throw new RuntimeException(e);
      }
    };
  }

  private static InputStream toInputStream(String input) throws IOException {
    return ByteSource.wrap(input.getBytes()).openBufferedStream();
  }

  private static String[] merge(String[] left, String... right) {
    return ObjectArrays.concat(left, right, String.class);
  }
}

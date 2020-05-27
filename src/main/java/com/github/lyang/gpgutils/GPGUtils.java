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
import java.util.Arrays;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPGUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(GPGUtils.class);
  private static final Consumer<InputStream> NOOP = stream -> {};

  public static int importKey(File key, String... options)
      throws IOException, InterruptedException {
    return processStream("--import", new FileInputStream(key), NOOP, options);
  }

  public static int encryptString(String input, StringBuilder builder, String... options)
      throws IOException, InterruptedException {
    return encryptStream(toInputStream(input), getWriter(builder), addArg("--armor", options));
  }

  public static int encryptFile(File input, File output, String... options)
      throws IOException, InterruptedException {
    return encryptStream(new FileInputStream(input), getWriter(output), options);
  }

  public static int encryptStream(
      InputStream stream, Consumer<InputStream> consumer, String... options)
      throws IOException, InterruptedException {
    return processStream("--encrypt", stream, consumer, options);
  }

  public static int decryptString(String input, StringBuilder builder, String... options)
      throws IOException, InterruptedException {
    return decryptStream(toInputStream(input), getWriter(builder), options);
  }

  public static int decryptFile(File input, File output, String... options)
      throws IOException, InterruptedException {
    return decryptStream(new FileInputStream(input), getWriter(output), options);
  }

  public static int decryptStream(
      InputStream stream, Consumer<InputStream> consumer, String... options)
      throws IOException, InterruptedException {
    return processStream("--decrypt", stream, consumer, options);
  }

  public static int processStream(
      String command, InputStream stream, Consumer<InputStream> consumer, String... options)
      throws IOException, InterruptedException {
    Process process = getGPGProcess(command, options).start();
    Thread writer = getWriterThread(process, stream);
    Thread reader = getReaderThread(process, consumer);
    writer.start();
    reader.start();
    writer.join();
    reader.join();
    return process.waitFor();
  }

  private static ProcessBuilder getGPGProcess(String command, String... options) {
    String[] commandLineArgs = addArg("gpg", addArg(command, options));
    return new ProcessBuilder(commandLineArgs).redirectError(ProcessBuilder.Redirect.INHERIT);
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

  private static Thread getReaderThread(Process process, Consumer<InputStream> consumer) {
    return new Thread(
        () -> {
          try (InputStream stream = process.getInputStream()) {
            consumer.accept(stream);
          } catch (IOException e) {
            LOGGER.error("Failed in reader thread", e);
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

  private static String[] addArg(String arg, String[] options) {
    if (Arrays.stream(options).anyMatch(arg::equalsIgnoreCase)) {
      return options;
    } else {
      return ObjectArrays.concat(arg, options);
    }
  }

  private static InputStream toInputStream(String input) throws IOException {
    return ByteSource.wrap(input.getBytes()).openBufferedStream();
  }
}

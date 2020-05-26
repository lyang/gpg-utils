package com.github.lyang.gpgutils;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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

  public static int encryptStream(
      InputStream stream, Consumer<InputStream> consumer, String... options)
      throws IOException, InterruptedException {
    return processStream("--encrypt", stream, consumer, options);
  }

  private static int processStream(
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
    List<String> commands =
        ImmutableList.<String>builder().add("gpg", command).add(options).build();
    return new ProcessBuilder(commands).redirectError(ProcessBuilder.Redirect.INHERIT);
  }

  private static Thread getWriterThread(Process process, InputStream inputStream) {
    return new Thread(
        () -> {
          try {
            ByteStreams.copy(inputStream, process.getOutputStream());
            process.getOutputStream().close();
          } catch (IOException e) {
            LOGGER.error("Failure in writer thread", e);
            throw new RuntimeException(e);
          }
        });
  }

  private static Thread getReaderThread(Process process, Consumer<InputStream> consumer) {
    return new Thread(
        () -> {
          try {
            consumer.accept(process.getInputStream());
            process.getInputStream().close();
          } catch (IOException e) {
            LOGGER.error("Failure in reader thread", e);
            throw new RuntimeException(e);
          }
        });
  }
}

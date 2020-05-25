package com.github.lyang.gpgutils;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPGUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(GPGUtils.class);

  public static int importKey(File key, String... options)
      throws IOException, InterruptedException {
    return getGPGProcess("--import", options).redirectInput(key).start().waitFor();
  }

  private static ProcessBuilder getGPGProcess(String command, String... options) {
    List<String> commands =
        ImmutableList.<String>builder().add("gpg", command).add(options).build();
    return new ProcessBuilder(commands).redirectError(ProcessBuilder.Redirect.INHERIT);
  }
}

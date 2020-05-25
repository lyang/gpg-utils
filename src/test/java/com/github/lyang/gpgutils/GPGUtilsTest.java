package com.github.lyang.gpgutils;

import static org.junit.Assert.assertEquals;

import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GPGUtilsTest {
  private File tempDir;
  private final File KEY = new File(Resources.getResource("private-key.gpg").getPath());

  @Before
  public void setUp() {
    tempDir = Files.createTempDir();
  }

  @After
  public void tearDown() throws Exception {
    MoreFiles.deleteRecursively(tempDir.toPath());
  }

  @Test
  public void importKey() throws IOException, InterruptedException {
    assertEquals(0, GPGUtils.importKey(KEY, "--homedir", tempDir.getAbsolutePath()));
  }
}

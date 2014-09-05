/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark_remote.api.java;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark_remote.impl.ClientUtils;

public class JavaSparkClientSuite {

  // Timeouts are bad... mmmkay.
  private static final long TIMEOUT = 10;

  private SparkConf createConf(boolean local) {
    if (local) {
      return new SparkConf()
        .set(ClientUtils.CONF_KEY_IN_PROCESS(), "true")
        .setMaster("local")
        .setAppName("JavaSparkClientSuite Local App");
    } else {
      String sparkHome = System.getProperty("spark.test.home");
      if (sparkHome == null) {
        fail("spark.test.home is not set!");
      }

      String classpath = System.getProperty("java.class.path");

      return new SparkConf()
        .setMaster("local")
        .setAppName("SparkClientSuite Remote App")
        .setSparkHome(sparkHome)
        .set("spark.driver.extraClassPath", classpath)
        .set("spark.executor.extraClassPath", classpath);
    }
  }

  @Test
  public void testJobSubmission() throws Exception {
    runTest(true, new TestFunction() {
      @Override
      public void call(JavaSparkClient client) throws Exception {
        JavaJobHandle<String> handle = client.submit(new SimpleJob());
        assertEquals("hello", handle.get(TIMEOUT, TimeUnit.SECONDS));
      }
    });
  }

  @Test
  public void testSimpleSparkJob() throws Exception {
    runTest(true, new TestFunction() {
      @Override
      public void call(JavaSparkClient client) throws Exception {
        JavaJobHandle<Long> handle = client.submit(new SparkJob());
        assertEquals(Long.valueOf(5L), handle.get(TIMEOUT, TimeUnit.SECONDS));
      }
    });
  }

  @Test
  public void testErrorJob() throws Exception {
    runTest(true, new TestFunction() {
      @Override
      public void call(JavaSparkClient client) throws Exception {
      JavaJobHandle<String> handle = client.submit(new SimpleJob());
        try {
          handle.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (ExecutionException ee) {
          assertTrue(ee.getCause() instanceof IllegalStateException);
        }
      }
    });
  }

  @Test
  public void testRemoteClient() throws Exception {
    runTest(false, new TestFunction() {
      @Override
      public void call(JavaSparkClient client) throws Exception {
        JavaJobHandle<String> handle = client.submit(new SimpleJob());
        assertEquals("hello", handle.get(TIMEOUT, TimeUnit.SECONDS));
      }
    });
  }

  private void runTest(boolean local, TestFunction test) throws Exception {
    SparkConf conf = createConf(local);
    JavaSparkClient.initialize(conf);
    JavaSparkClient client = null;
    try {
      client = JavaSparkClient.createClient(conf);
      test.call(client);
    } finally {
      if (client != null) {
        client.stop();
      }
      JavaSparkClient.uninitialize();
    }
  }

  private static class SimpleJob implements JavaJob<String> {

    @Override
    public String call(JavaJobContext jc) {
      return "hello";
    }

  }

  private static class SparkJob implements JavaJob<Long> {

    @Override
    public Long call(JavaJobContext jc) {
      JavaRDD<Integer> rdd = jc.sc().parallelize(Arrays.asList(1, 2, 3, 4, 5));
      return rdd.count();
    }

  }

  private static class ErrorJob implements JavaJob<String> {

    @Override
    public String call(JavaJobContext jc) {
      throw new IllegalStateException("This job does not work.");
    }

  }

  private static interface TestFunction {
    void call(JavaSparkClient client) throws Exception;
  }

}

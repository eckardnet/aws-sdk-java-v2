/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.checksums;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.checksums.Md5Checksum;
import software.amazon.awssdk.services.s3.internal.checksums.S3ChecksumValidatingPublisher;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Unit test for S3ChecksumValidatingPublisher
 */
public class S3ChecksumValidatingPublisherTest {
  private static int TEST_DATA_SIZE = 32;  // size of the test data, in bytes
  private static final int CHECKSUM_SIZE = 16;
  private static byte[] testData;
  private static byte[] testDataWithoutChecksum;

  @BeforeAll
  public static void populateData() {
    testData = new byte[TEST_DATA_SIZE + CHECKSUM_SIZE];
    for (int i = 0; i < TEST_DATA_SIZE; i++) {
      testData[i] = (byte)(i & 0x7f);
    }
    final Md5Checksum checksum = new Md5Checksum();
    checksum.update(testData, 0, TEST_DATA_SIZE);
    byte[] checksumBytes = checksum.getChecksumBytes();
    for (int i = 0; i < CHECKSUM_SIZE; i++) {
      testData[TEST_DATA_SIZE + i] = checksumBytes[i];
    }

    testDataWithoutChecksum = Arrays.copyOfRange(testData, 0, TEST_DATA_SIZE);
  }

  @Test
  public void testSinglePacket() {
    final TestPublisher driver = new TestPublisher();
    final TestSubscriber s = new TestSubscriber();
    final S3ChecksumValidatingPublisher p = new S3ChecksumValidatingPublisher(driver, new Md5Checksum(), TEST_DATA_SIZE + CHECKSUM_SIZE);
    p.subscribe(s);

    driver.doOnNext(ByteBuffer.wrap(testData));
    driver.doOnComplete();

    assertArrayEquals(testDataWithoutChecksum, s.receivedData());
    assertTrue(s.hasCompleted());
    assertFalse(s.isOnErrorCalled());
  }

  @Test
  public void testLastChecksumByteCorrupted() {
    TestPublisher driver = new TestPublisher();

    TestSubscriber s = new TestSubscriber();
    S3ChecksumValidatingPublisher p = new S3ChecksumValidatingPublisher(driver, new Md5Checksum(), TEST_DATA_SIZE + CHECKSUM_SIZE);
    p.subscribe(s);

    byte[] incorrectChecksumData = Arrays.copyOfRange(testData, 0, TEST_DATA_SIZE);
    incorrectChecksumData[TEST_DATA_SIZE - 1] = (byte) ~incorrectChecksumData[TEST_DATA_SIZE - 1];
    driver.doOnNext(ByteBuffer.wrap(incorrectChecksumData));
    driver.doOnComplete();

    assertFalse(s.hasCompleted());
    assertTrue(s.isOnErrorCalled());
  }

  @Test
  public void testTwoPackets() {
    for (int i = 1; i < TEST_DATA_SIZE + CHECKSUM_SIZE - 1; i++) {
      final TestPublisher driver = new TestPublisher();
      final TestSubscriber s = new TestSubscriber();
      final S3ChecksumValidatingPublisher p = new S3ChecksumValidatingPublisher(driver, new Md5Checksum(), TEST_DATA_SIZE + CHECKSUM_SIZE);
      p.subscribe(s);

      driver.doOnNext(ByteBuffer.wrap(testData, 0, i));
      driver.doOnNext(ByteBuffer.wrap(testData, i, TEST_DATA_SIZE + CHECKSUM_SIZE - i));
      driver.doOnComplete();

      assertArrayEquals(testDataWithoutChecksum, s.receivedData());
      assertTrue(s.hasCompleted());
      assertFalse(s.isOnErrorCalled());
    }
  }

  @Test
  public void testTinyPackets() {
    for (int packetSize = 1; packetSize < CHECKSUM_SIZE; packetSize++) {
      final TestPublisher driver = new TestPublisher();
      final TestSubscriber s = new TestSubscriber();
      final S3ChecksumValidatingPublisher p = new S3ChecksumValidatingPublisher(driver, new Md5Checksum(), TEST_DATA_SIZE + CHECKSUM_SIZE);
      p.subscribe(s);
      int currOffset = 0;
      while (currOffset < TEST_DATA_SIZE + CHECKSUM_SIZE) {
        final int toSend = Math.min(packetSize, TEST_DATA_SIZE + CHECKSUM_SIZE - currOffset);
        driver.doOnNext(ByteBuffer.wrap(testData, currOffset, toSend));
        currOffset += toSend;
      }
      driver.doOnComplete();

      assertArrayEquals(testDataWithoutChecksum, s.receivedData());
      assertTrue(s.hasCompleted());
      assertFalse(s.isOnErrorCalled());
    }
  }

  @Test
  public void testUnknownLength() {
    // When the length is unknown, the last 16 bytes are treated as a checksum, but are later ignored when completing
    final TestPublisher driver = new TestPublisher();
    final TestSubscriber s = new TestSubscriber();
    final S3ChecksumValidatingPublisher p = new S3ChecksumValidatingPublisher(driver, new Md5Checksum(), 0);
    p.subscribe(s);

    byte[] randomChecksumData = new byte[testData.length];
    System.arraycopy(testData, 0, randomChecksumData, 0, TEST_DATA_SIZE);
    for (int i = TEST_DATA_SIZE; i < randomChecksumData.length; i++) {
      randomChecksumData[i] = (byte)((testData[i] + 1) & 0x7f);
    }

    driver.doOnNext(ByteBuffer.wrap(randomChecksumData));
    driver.doOnComplete();

    assertArrayEquals(testDataWithoutChecksum, s.receivedData());
    assertTrue(s.hasCompleted());
    assertFalse(s.isOnErrorCalled());
  }

  @Test
  public void checksumValidationFailure_throwsSdkClientException_NotNPE() {
    final byte[] incorrectData = new byte[0];
    final TestPublisher driver = new TestPublisher();
    final TestSubscriber s = new TestSubscriber();
    final S3ChecksumValidatingPublisher p = new S3ChecksumValidatingPublisher(driver, new Md5Checksum(), TEST_DATA_SIZE + CHECKSUM_SIZE);
    p.subscribe(s);

    driver.doOnNext(ByteBuffer.wrap(incorrectData));
    driver.doOnComplete();

    assertTrue(s.isOnErrorCalled());
    assertFalse(s.hasCompleted());
  }

  private class TestSubscriber implements Subscriber<ByteBuffer> {
    final List<ByteBuffer> received;
    boolean completed;
    boolean onErrorCalled;

    TestSubscriber() {
      this.received = new ArrayList<>();
      this.completed = false;
    }

    @Override
    public void onSubscribe(Subscription s) {
      fail("This method not expected to be invoked");
      throw new UnsupportedOperationException("!!!TODO: implement this");
    }

    @Override
    public void onNext(ByteBuffer buffer) {
      received.add(buffer);
    }


    @Override
    public void onError(Throwable t) {
      onErrorCalled = true;
    }

    @Override
    public void onComplete() {
      completed = true;
    }

    public byte[] receivedData() {
      try {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (ByteBuffer buffer : received) {
          os.write(BinaryUtils.copyBytesFrom(buffer));
        }
        return os.toByteArray();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    public boolean hasCompleted() {
      return completed;
    }

    public boolean isOnErrorCalled() {
      return onErrorCalled;
    }
  }

  private class TestPublisher implements Publisher<ByteBuffer> {
    Subscriber<? super ByteBuffer> s;

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> s) {
      this.s = s;
    }

    public void doOnNext(ByteBuffer b) {
      s.onNext(b);
    }

    public void doOnComplete() {
      s.onComplete();
    }
  }
}

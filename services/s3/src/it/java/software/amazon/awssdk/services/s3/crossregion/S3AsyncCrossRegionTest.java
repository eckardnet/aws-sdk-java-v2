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

package software.amazon.awssdk.services.s3.crossregion;

import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;

public class S3AsyncCrossRegionTest extends S3CrossRegionTestBase {
    private S3AsyncClient crossRegionS3Client ;

    @BeforeEach
    public void initialize() {
        captureInterceptor = new CaptureInterceptor();
        crossRegionS3Client = S3AsyncClient.builder()
                                          .region(CROSS_REGION)
                                           .serviceConfiguration(s -> s.crossRegionAccessEnabled(true))
                                           .overrideConfiguration(o -> o.addExecutionInterceptor(captureInterceptor))
                                           .build();
    }

    @AfterEach
    void reset(){
        captureInterceptor.reset();
    }
    @BeforeAll
    static void setUpClass(){
        // Bucket Created in CROSS Region
        s3 = s3ClientBuilder().build();
        createBucket(BUCKET);
    }

    @AfterAll
    static void clearClass(){
        deleteBucketAndAllContents(BUCKET);
    }

    private static final String BUCKET = temporaryBucketName(S3AsyncCrossRegionTest.class);

    @Override
    protected List<S3Object> paginatedAPICall(ListObjectsV2Request listObjectsV2Request) {
        List<S3Object> resultObjects = new ArrayList<>();
        ListObjectsV2Publisher publisher = crossRegionS3Client.listObjectsV2Paginator(listObjectsV2Request);
        CompletableFuture<Void> subscribe = publisher.subscribe(response -> {
             response.contents().forEach(a -> resultObjects.add(a));
        });
        subscribe.join();
        return resultObjects;
    }

    @Override
    protected DeleteObjectsResponse postObjectAPICall(DeleteObjectsRequest deleteObjectsRequest) {
        return crossRegionS3Client.deleteObjects(deleteObjectsRequest).join();
    }

    @Override
    protected HeadBucketResponse headAPICall(HeadBucketRequest headBucketRequest) {
        return crossRegionS3Client.headBucket(headBucketRequest).join();
    }

    @Override
    protected DeleteObjectResponse deleteObjectAPICall(DeleteObjectRequest deleteObjectRequest) {
        return crossRegionS3Client.deleteObject(deleteObjectRequest).join();
    }

    @Override
    protected PutObjectResponse putAPICall(PutObjectRequest putObjectRequest, String testString) {
        return crossRegionS3Client.putObject(putObjectRequest, AsyncRequestBody.fromString(testString)).join();
    }

    @Override
    protected ResponseBytes<GetObjectResponse> getAPICall(GetObjectRequest getObjectRequest) {
        return crossRegionS3Client.getObject(getObjectRequest, AsyncResponseTransformer.toBytes()).join();
    }

    @Override
    protected String bucketName() {
        return BUCKET;
    }

}

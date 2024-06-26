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

package software.amazon.awssdk.services.cloudfront;

import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class IntegrationTestBase extends AwsIntegrationTestBase {

    protected static CloudFrontClient cloudFrontClient;
    protected static CloudFrontUtilities cloudFrontUtilities;
    protected static S3Client s3Client;
    protected static SecretsManagerClient secretsManagerClient;

    /**
     * Loads the AWS account info for the integration tests and creates an
     * AutoScaling client for tests to use.
     */
    @BeforeClass
    public static void setUp() {
        cloudFrontClient = CloudFrontClient.builder()
                                           .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                           .region(Region.AWS_GLOBAL)
                                           .build();

        cloudFrontUtilities = cloudFrontClient.utilities();

        s3Client = S3Client.builder()
                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                     .region(Region.US_EAST_1)
                     .build();

        secretsManagerClient = SecretsManagerClient.builder()
                                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                   .region(Region.US_EAST_1)
                                                   .build();
    }

    /**
     * Deletes all objects in the specified bucket, and then deletes the bucket.
     *
     * @param bucketName
     *            The bucket to empty and delete.
     */
    protected static void deleteBucketAndAllContents(String bucketName) {
        ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());

        for (S3Object obj: listObjectsResponse.contents()) {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(obj.key()).build());
        }

        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }

}

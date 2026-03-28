package com.deli.delivery.s3

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.time.Duration
import java.time.Instant

@Service
class S3Service(
    @Value("\${deli.s3.endpoint}") private val endpoint: String,
    @Value("\${deli.s3.access-key}") private val accessKey: String,
    @Value("\${deli.s3.secret-key}") private val secretKey: String,
    @Value("\${deli.s3.bucket-photos}") private val bucketPhotos: String,
    @Value("\${deli.s3.bucket-signatures}") private val bucketSignatures: String,
    @Value("\${deli.s3.presigned-url-expiry-seconds:300}") private val presignedUrlExpiry: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val credentials =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey),
        )

    private val presigner: S3Presigner =
        S3Presigner
            .builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1) // MinIO ignores region but SDK requires one
            .credentialsProvider(credentials)
            .build()

    private val s3Client: S3Client =
        S3Client
            .builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .forcePathStyle(true) // Required for MinIO — virtual-hosted style fails
            .build()

    // ── Pre-signed upload URLs ────────────────────────────────────────────────

    fun generatePhotoUploadUrl(
        stopId: String,
        contentType: String,
    ): PresignedUpload {
        val key = "stops/$stopId/proof-photo.${contentType.substringAfter('/')}"
        return generatePresignedPut(bucketPhotos, key, contentType)
    }

    fun generateSignatureUploadUrl(stopId: String): PresignedUpload {
        val key = "stops/$stopId/signature.png"
        return generatePresignedPut(bucketSignatures, key, "image/png")
    }

    // ── Pre-signed download URLs ──────────────────────────────────────────────

    fun generatePhotoDownloadUrl(key: String): String = generatePresignedGet(bucketPhotos, key)

    fun generateSignatureDownloadUrl(key: String): String = generatePresignedGet(bucketSignatures, key)

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun generatePresignedPut(
        bucket: String,
        key: String,
        contentType: String,
    ): PresignedUpload {
        val putRequest =
            PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build()

        val presignRequest =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlExpiry))
                .putObjectRequest(putRequest)
                .build()

        val url = presigner.presignPutObject(presignRequest).url().toString()
        val expiresAt = Instant.now().plusSeconds(presignedUrlExpiry)

        log.debug("Generated pre-signed PUT URL for $bucket/$key")
        return PresignedUpload(uploadUrl = url, fileKey = key, expiresAt = expiresAt)
    }

    private fun generatePresignedGet(
        bucket: String,
        key: String,
    ): String {
        val getRequest =
            software.amazon.awssdk.services.s3.model.GetObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .build()

        val presignRequest =
            software.amazon.awssdk.services.s3.presigner.model
                .GetObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlExpiry))
                .getObjectRequest(getRequest)
                .build()

        return presigner.presignGetObject(presignRequest).url().toString()
    }
}

data class PresignedUpload(
    val uploadUrl: String,
    val fileKey: String,
    val expiresAt: Instant,
)

package com.deli.delivery.s3

import com.deli.shared.domain.model.InvalidImageException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
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

    // ── Post-upload validation ────────────────────────────────────────────────

    /**
     * Downloads the first 8 bytes of a photo object and checks its magic bytes.
     * Throws [InvalidImageException] if the file header does not match the expected image format.
     * The caller is responsible for deleting the object if validation fails.
     */
    fun validatePhotoMagicBytes(key: String) {
        val extension = key.substringAfterLast('.')
        val expectedContentType =
            when (extension) {
                "jpeg", "jpg" -> "image/jpeg"
                "png" -> "image/png"
                else -> throw InvalidImageException("Unsupported file type: $extension")
            }

        val getRequest =
            GetObjectRequest
                .builder()
                .bucket(bucketPhotos)
                .key(key)
                .range("bytes=0-7")
                .build()

        val bytes = s3Client.getObjectAsBytes(getRequest).asByteArray()
        checkMagicBytes(bytes, expectedContentType)
    }

    fun deletePhoto(key: String) {
        s3Client.deleteObject(
            DeleteObjectRequest
                .builder()
                .bucket(bucketPhotos)
                .key(key)
                .build(),
        )
        log.debug("Deleted invalid photo object: $bucketPhotos/$key")
    }

    // ── Private helpers ─────────────────────────────────────��─────────────────

    private fun checkMagicBytes(
        bytes: ByteArray,
        contentType: String,
    ) {
        val magic =
            when (contentType) {
                "image/jpeg" -> byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

                "image/png" -> byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

                // \x89PNG
                else -> throw InvalidImageException("Unsupported content type: $contentType")
            }
        if (bytes.size < magic.size || !bytes.take(magic.size).toByteArray().contentEquals(magic)) {
            throw InvalidImageException("File header does not match declared content type $contentType")
        }
    }

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

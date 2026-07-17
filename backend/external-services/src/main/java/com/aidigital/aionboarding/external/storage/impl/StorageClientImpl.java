package com.aidigital.aionboarding.external.storage.impl;

import com.aidigital.aionboarding.external.common.http.ExternalCallTimer;
import com.aidigital.aionboarding.external.storage.StorageClient;
import com.aidigital.aionboarding.external.storage.StorageExternalException;
import com.aidigital.aionboarding.external.storage.config.StorageProperties;
import com.aidigital.aionboarding.external.storage.models.ObjectMetadataRecord;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Production {@link StorageClient} backed by AWS SDK v2 S3. GET URLs are served through
 * CloudFront (signed by {@link CloudFrontUrlSigner}) when configured; PUT URLs always go
 * directly to S3, since a GET-only CloudFront distribution cannot proxy uploads.
 */
@RequiredArgsConstructor
public class StorageClientImpl implements StorageClient {

    private static final Logger LOG = LoggerFactory.getLogger(StorageClientImpl.class);
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final StorageProperties properties;
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final ExternalCallTimer externalCallTimer;
    private final Optional<CloudFrontUrlSigner> cloudFrontUrlSigner;

    @Override
    public String presignPut(String storageKey, String contentType, Duration expiresIn) {
        requireKey(storageKey);
        Duration lifetime = resolveDuration(expiresIn, properties.getPresignPutExpiresSeconds());
        String resolvedContentType = resolveContentType(contentType);

        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .contentType(resolvedContentType)
                .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(lifetime)
                .putObjectRequest(objectRequest)
                .build();

            PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception ex) {
            throw new StorageExternalException("Failed to presign PUT for key " + storageKey, ex);
        }
    }

    @Override
    public String presignGet(String storageKey, Duration expiresIn) {
        requireKey(storageKey);
        Duration lifetime = resolveDuration(expiresIn, properties.getPresignGetExpiresSeconds());

        if (cloudFrontUrlSigner.isPresent()) {
            return cloudFrontUrlSigner.get().sign(storageKey, lifetime);
        }

        try {
            GetObjectRequest.Builder objectRequestBuilder = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(storageKey)
                .responseContentDisposition("inline");
            String responseContentType = inferContentType(storageKey);
            if (responseContentType != null) {
                objectRequestBuilder.responseContentType(responseContentType);
            }
            GetObjectRequest objectRequest = objectRequestBuilder.build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(lifetime)
                .getObjectRequest(objectRequest)
                .build();

            PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception ex) {
            throw new StorageExternalException("Failed to presign GET for key " + storageKey, ex);
        }
    }

    @Override
    public void putObject(String storageKey, byte[] content, String contentType) {
        requireKey(storageKey);
        if (content == null) {
            throw new StorageExternalException("Object content must not be null");
        }

        externalCallTimer.record("s3", "putObject", () -> {
            try {
                PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .contentType(resolveContentType(contentType))
                    .contentDisposition("inline")
                    .build();

                s3Client.putObject(request, RequestBody.fromBytes(content));
                LOG.debug("Uploaded object to storage key={}", storageKey);
            } catch (Exception ex) {
                throw new StorageExternalException("Failed to upload object for key " + storageKey, ex);
            }
        });
    }

    @Override
    public void putObjectStreaming(String storageKey, InputStream content, long contentLength, String contentType) {
        requireKey(storageKey);
        if (content == null) {
            throw new StorageExternalException("Object content must not be null");
        }

        externalCallTimer.record("s3", "putObjectStreaming", () -> {
            try {
                PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .contentType(resolveContentType(contentType))
                    .contentDisposition("inline")
                    .build();

                s3Client.putObject(request, RequestBody.fromInputStream(content, contentLength));
                LOG.debug("Streamed object to storage key={}", storageKey);
            } catch (Exception ex) {
                throw new StorageExternalException("Failed to stream object for key " + storageKey, ex);
            }
        });
    }

    @Override
    public Optional<ObjectMetadataRecord> headObject(String storageKey) {
        requireKey(storageKey);

        return externalCallTimer.record("s3", "headObject", () -> {
            try {
                HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build();

                HeadObjectResponse response = s3Client.headObject(request);
                return Optional.of(new ObjectMetadataRecord(response.contentLength(), response.contentType()));
            } catch (NoSuchKeyException ex) {
                return Optional.empty();
            } catch (Exception ex) {
                throw new StorageExternalException("Failed to look up object for key " + storageKey, ex);
            }
        });
    }

    @Override
    public byte[] getObjectBuffer(String storageKey, long maxSizeBytes) {
        requireKey(storageKey);

        return externalCallTimer.record("s3", "getObject", () -> {
            try {
                GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(storageKey)
                    .build();

                try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
                    Long declaredLength = response.response().contentLength();
                    if (declaredLength != null && declaredLength > maxSizeBytes) {
                        throw new StorageExternalException("Object exceeds the maximum allowed size for this operation.");
                    }
                    // Reads one byte past the cap so an object whose declared length under-reports
                    // its real size is still caught by an actual byte count, not just the header.
                    byte[] bytes = response.readNBytes(Math.toIntExact(maxSizeBytes) + 1);
                    if (bytes.length > maxSizeBytes) {
                        throw new StorageExternalException("Object exceeds the maximum allowed size for this operation.");
                    }
                    return bytes;
                }
            } catch (IOException ex) {
                throw new StorageExternalException("Failed to read object for key " + storageKey, ex);
            } catch (Exception ex) {
                throw new StorageExternalException("Failed to download object for key " + storageKey, ex);
            }
        });
    }

    @Override
    public void deleteObjects(List<String> storageKeys) {
        Set<String> uniqueKeys = new LinkedHashSet<>();
        if (storageKeys != null) {
            for (String key : storageKeys) {
                if (key != null && !key.isBlank()) {
                    uniqueKeys.add(key);
                }
            }
        }

        if (uniqueKeys.isEmpty()) {
            return;
        }

        externalCallTimer.record("s3", "deleteObjects", () -> {
            try {
                List<ObjectIdentifier> objects = uniqueKeys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();

                DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(properties.getBucket())
                    .delete(Delete.builder().objects(objects).quiet(true).build())
                    .build();

                s3Client.deleteObjects(request);
                LOG.debug("Deleted {} storage object(s)", uniqueKeys.size());
            } catch (Exception ex) {
                throw new StorageExternalException("Failed to delete storage objects", ex);
            }
        });
    }

    private void requireKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new StorageExternalException("Storage key is required");
        }
    }

    private Duration resolveDuration(Duration expiresIn, int defaultSeconds) {
        if (expiresIn != null && !expiresIn.isZero() && !expiresIn.isNegative()) {
            return expiresIn;
        }
        return Duration.ofSeconds(defaultSeconds);
    }

    private String resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    private String inferContentType(String storageKey) {
        String lowerKey = storageKey.toLowerCase();
        if (lowerKey.endsWith(".mp4") || lowerKey.endsWith(".m4v")) {
            return "video/mp4";
        }
        if (lowerKey.endsWith(".webm")) {
            return "video/webm";
        }
        if (lowerKey.endsWith(".mov")) {
            return "video/quicktime";
        }
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerKey.endsWith(".png")) {
            return "image/png";
        }
        if (lowerKey.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerKey.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerKey.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lowerKey.endsWith(".pdf")) {
            return "application/pdf";
        }
        return null;
    }
}

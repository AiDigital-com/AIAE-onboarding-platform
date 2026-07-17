package com.aidigital.aionboarding.external.storage.impl;

import com.aidigital.aionboarding.external.common.http.ExternalCallTimer;
import com.aidigital.aionboarding.external.storage.StorageExternalException;
import com.aidigital.aionboarding.external.storage.config.StorageProperties;
import com.aidigital.aionboarding.external.storage.models.ObjectMetadataRecord;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageClientImplTest {

	@Mock
	private S3Client s3Client;
	@Mock
	private S3Presigner presigner;
	@Mock
	private StorageProperties properties;
	@Mock
	private ExternalCallTimer externalCallTimer;
	@Mock
	private Optional<CloudFrontUrlSigner> cloudFrontUrlSigner;
	@Mock
	private CloudFrontUrlSigner cloudFrontUrlSignerImpl;

	@InjectMocks
	private StorageClientImpl client;

	@Test
	void shouldReturnPresignPutUrlTest() throws Exception {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		when(properties.getPresignPutExpiresSeconds()).thenReturn(300);
		PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
		when(presigned.url()).thenReturn(URI.create("https://s3.example.com/b/key").toURL());
		when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

		// When:
		String url = client.presignPut("uploads/test.txt", "text/plain", Duration.ofMinutes(5));

		// Then:
		assertThat(url).isEqualTo("https://s3.example.com/b/key");
	}

	@Test
	void shouldThrowPresignPutForNullKeyTest() {
		// When / Then:
		assertThatThrownBy(() -> client.presignPut(null, "text/plain", Duration.ofMinutes(5)))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Storage key is required");
	}

	@Test
	void shouldThrowPresignPutForBlankKeyTest() {
		// When / Then:
		assertThatThrownBy(() -> client.presignPut("  ", "text/plain", Duration.ofMinutes(5)))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Storage key is required");
	}

	@Test
	void shouldReturnPresignGetUrlTest() throws Exception {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		when(properties.getPresignGetExpiresSeconds()).thenReturn(600);
		PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
		when(presigned.url()).thenReturn(URI.create("https://cf.example.com/key").toURL());
		when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

		// When:
		String url = client.presignGet("uploads/video.mp4", Duration.ofMinutes(10));

		// Then:
		assertThat(url).isEqualTo("https://cf.example.com/key");
	}

	@Test
	void shouldThrowPresignGetForNullKeyTest() {
		// When / Then:
		assertThatThrownBy(() -> client.presignGet(null, Duration.ofMinutes(10)))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Storage key is required");
	}

	@Test
	void shouldUseCloudFrontSignerWhenConfiguredTest() throws Exception {
		// Given:
		when(properties.getPresignGetExpiresSeconds()).thenReturn(600);
		when(cloudFrontUrlSigner.isPresent()).thenReturn(true);
		when(cloudFrontUrlSigner.get()).thenReturn(cloudFrontUrlSignerImpl);
		when(cloudFrontUrlSignerImpl.sign("uploads/video.mp4", Duration.ofSeconds(600)))
				.thenReturn("https://cf.example.com/signed-url");

		// When:
		String url = client.presignGet("uploads/video.mp4", null);

		// Then:
		assertThat(url).isEqualTo("https://cf.example.com/signed-url");
	}

	@Test
	void shouldThrowPutObjectForNullKeyTest() {
		// When / Then:
		assertThatThrownBy(() -> client.putObject(null, new byte[0], "text/plain"))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Storage key is required");
	}

	@Test
	void shouldThrowPutObjectForNullContentTest() {
		// When / Then:
		assertThatThrownBy(() -> client.putObject("test.txt", null, "text/plain"))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Object content must not be null");
	}

	@Test
	void shouldPutObjectTest() {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		doAnswer(invocation -> {
			invocation.getArgument(2, Runnable.class).run();
			return null;
		}).when(externalCallTimer).record(anyString(), anyString(), any(Runnable.class));

		// When:
		client.putObject("test.txt", new byte[]{1, 2, 3}, "text/plain");

		// Then: no exception is thrown
	}

	@Test
	void shouldThrowPutObjectStreamingForNullKeyTest() {
		// When / Then:
		assertThatThrownBy(() -> client.putObjectStreaming(null, java.io.InputStream.nullInputStream(), 10, "text" +
				"/plain"))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Storage key is required");
	}

	@Test
	void shouldThrowPutObjectStreamingForNullContentTest() {
		// When / Then:
		assertThatThrownBy(() -> client.putObjectStreaming("test.txt", null, 10, "text/plain"))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Object content must not be null");
	}

	@Test
	void shouldPutObjectStreamingTest() {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		doAnswer(invocation -> {
			invocation.getArgument(2, Runnable.class).run();
			return null;
		}).when(externalCallTimer).record(anyString(), anyString(), any(Runnable.class));

		// When:
		client.putObjectStreaming("test.mp4", java.io.InputStream.nullInputStream(), 100, "video/mp4");

		// Then: no exception is thrown
	}

	@Test
	void shouldThrowHeadObjectForNullKeyTest() {
		// When / Then:
		assertThatThrownBy(() -> client.headObject(null))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Storage key is required");
	}

	@Test
	void shouldReturnEmptyHeadObjectForNoSuchKeyTest() {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		doAnswer(invocation -> invocation.getArgument(2, Supplier.class).get())
				.when(externalCallTimer).record(anyString(), anyString(), any(Supplier.class));
		when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(
				NoSuchKeyException.builder().message("not found").build());

		// When:
		Optional<ObjectMetadataRecord> result = client.headObject("missing.txt");

		// Then:
		assertThat(result).isEmpty();
	}

	@Test
	void shouldReturnMetadataHeadObjectWhenPresentTest() {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		doAnswer(invocation -> invocation.getArgument(2, Supplier.class).get())
				.when(externalCallTimer).record(anyString(), anyString(), any(Supplier.class));
		HeadObjectResponse response = HeadObjectResponse.builder()
				.contentLength(100L)
				.contentType("text/plain")
				.build();
		when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(response);
		ObjectMetadataRecord expected = Instancio.of(ObjectMetadataRecord.class)
				.set(field("sizeBytes"), 100L)
				.set(field("contentType"), "text/plain")
				.create();

		// When:
		Optional<ObjectMetadataRecord> result = client.headObject("present.txt");

		// Then:
		assertThat(result).contains(expected);
	}

	@Test
	void shouldThrowGetObjectBufferForNullKeyTest() {
		// When / Then:
		assertThatThrownBy(() -> client.getObjectBuffer(null, 1024))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Storage key is required");
	}

	@Test
	void shouldReturnGetObjectBufferBytesTest() throws Exception {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		doAnswer(invocation -> invocation.getArgument(2, Supplier.class).get())
				.when(externalCallTimer).record(anyString(), anyString(), any(Supplier.class));
		GetObjectResponse getResp = GetObjectResponse.builder()
				.contentLength(3L)
				.build();
		@SuppressWarnings("unchecked")
		ResponseInputStream<GetObjectResponse> resp =
				(ResponseInputStream<GetObjectResponse>) mock(ResponseInputStream.class);
		when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(resp);
		when(resp.response()).thenReturn(getResp);
		when(resp.readNBytes(1025)).thenReturn(new byte[]{1, 2, 3});

		// When:
		byte[] result = client.getObjectBuffer("test.bin", 1024);

		// Then:
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	void shouldDeleteObjectsTest() {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		doAnswer(invocation -> {
			invocation.getArgument(2, Runnable.class).run();
			return null;
		}).when(externalCallTimer).record(anyString(), anyString(), any(Runnable.class));
		DeleteObjectsResponse response = DeleteObjectsResponse.builder().build();
		when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(response);

		// When:
		client.deleteObjects(List.of("key1", "key2"));

		// Then: no exception is thrown
	}

	@Test
	void shouldSkipNullAndBlankKeysWhenDeletingObjectsTest() {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		doAnswer(invocation -> {
			invocation.getArgument(2, Runnable.class).run();
			return null;
		}).when(externalCallTimer).record(anyString(), anyString(), any(Runnable.class));
		DeleteObjectsResponse response = DeleteObjectsResponse.builder().build();
		when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(response);

		// When:
		client.deleteObjects(java.util.Arrays.asList("key1", null, "  ", "key2"));

		// Then: no exception is thrown
	}

	@Test
	void shouldDoNothingWhenDeletingEmptyKeyListTest() {
		// When:
		client.deleteObjects(List.of());

		// Then: no exception is thrown
	}

	@Test
	void shouldInferVideoContentTypeForPresignGetTest() throws Exception {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		when(properties.getPresignGetExpiresSeconds()).thenReturn(600);
		PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
		when(presigned.url()).thenReturn(URI.create("https://cf.example.com/key").toURL());
		when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

		// When:
		String url = client.presignGet("uploads/video.mp4", Duration.ofMinutes(10));

		// Then:
		assertThat(url).isEqualTo("https://cf.example.com/key");
	}

	@Test
	void shouldDefaultPresignPutExpiryWhenNullTest() throws Exception {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		when(properties.getPresignPutExpiresSeconds()).thenReturn(300);
		PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
		when(presigned.url()).thenReturn(URI.create("https://s3.example.com/b/key").toURL());
		when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

		// When:
		String url = client.presignPut("uploads/test.txt", null, null);

		// Then:
		assertThat(url).isEqualTo("https://s3.example.com/b/key");
	}

	@Test
	void shouldReturnPresignGetUrlWithDefaultExpiryTest() throws Exception {
		// Given:
		when(properties.getBucket()).thenReturn("test-bucket");
		when(properties.getPresignGetExpiresSeconds()).thenReturn(600);
		PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
		when(presigned.url()).thenReturn(URI.create("https://cf.example.com/key").toURL());
		when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

		// When:
		String url = client.presignGet("uploads/video.mp4", null);

		// Then:
		assertThat(url).isEqualTo("https://cf.example.com/key");
	}
}

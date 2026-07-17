package com.aidigital.aionboarding.external.storage.impl;

import com.aidigital.aionboarding.external.storage.StorageExternalException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubStorageClientTest {

	@Test
	void shouldThrowPresignPutTest() {
		// Given:
		StubStorageClient client = new StubStorageClient();

		// When / Then:
		assertThatThrownBy(() -> client.presignPut("k", "text/plain", null))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Object storage is not configured");
	}

	@Test
	void shouldThrowPresignGetTest() {
		// Given:
		StubStorageClient client = new StubStorageClient();

		// When / Then:
		assertThatThrownBy(() -> client.presignGet("k", null))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Object storage is not configured");
	}

	@Test
	void shouldThrowPutObjectTest() {
		// Given:
		StubStorageClient client = new StubStorageClient();

		// When / Then:
		assertThatThrownBy(() -> client.putObject("k", new byte[]{1, 2, 3}, "text/plain"))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Object storage is not configured");
	}

	@Test
	void shouldThrowPutObjectStreamingTest() {
		// Given:
		StubStorageClient client = new StubStorageClient();

		// When / Then:
		assertThatThrownBy(() -> client.putObjectStreaming("k", new ByteArrayInputStream(new byte[]{1}), 1L,
				"application/octet-stream"))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Object storage is not configured");
	}

	@Test
	void shouldThrowHeadObjectTest() {
		// Given:
		StubStorageClient client = new StubStorageClient();

		// When / Then:
		assertThatThrownBy(() -> client.headObject("k"))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Object storage is not configured");
	}

	@Test
	void shouldThrowGetObjectBufferTest() {
		// Given:
		StubStorageClient client = new StubStorageClient();

		// When / Then:
		assertThatThrownBy(() -> client.getObjectBuffer("k", 1024L))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Object storage is not configured");
	}

	@Test
	void shouldThrowDeleteObjectsTest() {
		// Given:
		StubStorageClient client = new StubStorageClient();

		// When / Then:
		assertThatThrownBy(() -> client.deleteObjects(List.of("k1", "k2")))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Object storage is not configured");
	}
}

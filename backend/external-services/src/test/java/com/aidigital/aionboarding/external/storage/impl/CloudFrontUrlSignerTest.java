package com.aidigital.aionboarding.external.storage.impl;

import com.aidigital.aionboarding.external.storage.StorageExternalException;
import com.aidigital.aionboarding.external.storage.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.PrivateKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudFrontUrlSignerTest {

	private static final String PKCS1_PRIVATE_KEY = """
			-----BEGIN RSA PRIVATE KEY-----
			MIIEowIBAAKCAQEA7KUjaA92cXdHu35HiNMRYvPuMKc1kWKM4PbJrJ1s4Kwulq0Q
			fRFssjyckj76R0F0UIVOzZbwwuR9HxDYmcKdt8N56GccbTpRkRkMnzN+2vc8kCNr
			+uldz6fJ3WwnkJebEZi6ObPebjD6Qb0xltACcD4L2GUZPObTeU4xICR1hjtwxFUv
			h7uIcJzqaIMMjwN8djMyWUaHSetqYRbx3Yc1CW/S/GCluKb6oHMLP5tZdhxsX25l
			k/bBOp+TnlwUPHX9BzFJc2YQ7Mx7tERieJJaZks6KFQklHuisoZzDT6o92sXAmKD
			UrXCgnGbZve3IN+0lD1iA+ofA99982TYg0GnBwIDAQABAoIBAAW5gL4LawxCMZn7
			61UzEQobYRdvqIXOVeXIZhxxmkHVykC6wu8vKI7vBsNGQto/335JnCqwepLfDEDu
			MKyI4oqgqMZ5MzJYIcNWpoMghQ8HwfnqcI9UJnKmC/QIPnuqsBrrQhPMtw8rPHqV
			3D5ocwBt/u3qsC3i0ETwd1pR+wYPm86Cd/UUBEo3kMp8xobctJMsisbnNIQDsF4W
			+UJ4IqDzdh0izwpoDN0Nr5wtTD5mfnd7NhQGZ2W3Wo+DMMeyd1UOaLp5e8Zse92V
			q4nRws6Oq0e/QlpdebOJ1EjVg/LEaGNpeK/3SW0U9ONN/Ewave+u/I7WRZPNLGjr
			YUKkpv0CgYEA+Y8LzqQJkDJ8iMCAsMBe8rR+C9j1j9JdQRDxElQnL3SoycHomNvB
			Q1Gm5CD+/t5tOPfUvRRCSCjOvLw7DAMX84CEKWq+/dlPl0x0QJxAYg40+3/wuRw+
			3L4T1eOIcj9fuzZu04YAA10PsqM1j15X79rvSD/RR1PhsSflbPzSpfMCgYEA8sDD
			46Gltb/JeJFyJD8nLBgPR3Ehr4fQMlZq0ftiVqChngeJRcinShkuD4xpEAfxTR53
			5oC4uD2WsyEbwUGW9hLgwo8OEqtieKewOur1Axo++wNx1nHHemjSMxe2dtUfwo87
			soKNbB/bgSVBc9zYKISorQFndWaBwynHbTU5250CgYBqAiCy262QSlHqSVOhQWZT
			1OYCJFxThrnETO0KqyKmHvkgEzW50Qesj//DwlxvaY6d93CwDO4G9way14aBmMLQ
			1hPOACE3ddtAWuK7G2SiAoTxHKzmBSCS5k9IuUmZtl/1B69WPQK+awajDH9Q8nOK
			WMbyOuGsWe5yMY9cl5znjwKBgGhSNtyoJxKILQjHWIoDKc5lOIu3LobEktBPGvDK
			W2Rm+41yJ6f5pwrM72J3MN9Wyngd0+EXCEsFDLJGVCslCL8PNc/msmGLjXHUfoOT
			XV/L86zjuhQyKUuNqGeUlTFUPaXa8Aiy2hvRRP+nBw3Hpo0jFWnj5JYrSzCXz8A6
			03ZxAoGBAKRI1coHPJkZIBwjBcC6BhCCfcg/Q1fcgeOQHY2ab9HTIz319UKhUYxD
			fWrr68NEC3YTKEh0AzGXjRVku+YqMQh4ZAMUtRgvaohVsLQ8stvZjL8Ii+D1FLzs
			0Pg9UovNktTrnZQbFicF7dDluCBJgcSnSJrdXTFv++8iXlWPSlpe
			-----END RSA PRIVATE KEY-----
			""";

	private static final String INVALID_KEY = """
			-----BEGIN RSA PRIVATE KEY-----
			bm90LWEtdmFsaWQta2V5
			-----END RSA PRIVATE KEY-----
			""";

	@Mock
	private StorageProperties properties;

	@Test
	void shouldParsePkcs1PrivateKeyTest() {
		// Given:
		when(properties.getCloudFrontPrivateKey()).thenReturn(PKCS1_PRIVATE_KEY);

		// When:
		PrivateKey result = new CloudFrontUrlSigner(properties).parsePrivateKey(PKCS1_PRIVATE_KEY);

		// Then:
		assertThat(result).isNotNull();
		assertThat(result.getAlgorithm()).isEqualTo("RSA");
	}

	@Test
	void shouldThrowWhenPrivateKeyIsInvalidTest() {
		// Given:
		when(properties.getCloudFrontPrivateKey()).thenReturn(INVALID_KEY);

		// When / Then:
		assertThatThrownBy(() -> new CloudFrontUrlSigner(properties))
				.isInstanceOf(StorageExternalException.class)
				.hasMessageContaining("Failed to parse CloudFront private key");
	}
}

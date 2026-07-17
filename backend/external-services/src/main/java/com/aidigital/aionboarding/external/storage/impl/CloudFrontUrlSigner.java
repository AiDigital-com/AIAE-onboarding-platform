package com.aidigital.aionboarding.external.storage.impl;

import com.aidigital.aionboarding.external.storage.StorageExternalException;
import com.aidigital.aionboarding.external.storage.config.StorageProperties;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CannedSignerRequest;
import software.amazon.awssdk.services.cloudfront.url.SignedUrl;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

/**
 * Signs CloudFront resource URLs with a canned (single-expiry) policy using the key pair
 * configured in {@link StorageProperties}. The private key is parsed once at construction
 * time since {@code StorageProperties} does not change after Spring Boot startup; the bean
 * is only registered ({@code StorageConfig}) once the CloudFront key pair is fully configured.
 */
public class CloudFrontUrlSigner {

	private final StorageProperties properties;
	private final CloudFrontUtilities cloudFrontUtilities;
	private final PrivateKey privateKey;

	/**
	 * Parses the configured PEM private key and prepares the signer.
	 *
	 * @param properties storage properties holding the CloudFront domain/key-pair-id/private-key
	 */
	public CloudFrontUrlSigner(StorageProperties properties) {
		this.properties = properties;
		this.cloudFrontUtilities = CloudFrontUtilities.create();
		this.privateKey = parsePrivateKey(properties.getCloudFrontPrivateKey());
	}

	/**
	 * Builds a CloudFront-signed URL for the given storage key, expiring after {@code expiresIn}.
	 *
	 * @param storageKey object key relative to the distribution origin
	 * @param expiresIn  signature lifetime
	 * @return signed https URL served through CloudFront
	 */
	public String sign(String storageKey, Duration expiresIn) {
		try {
			String resourceUrl = "https://" + normalizedDomain() + "/" + storageKey;
			CannedSignerRequest request = CannedSignerRequest.builder()
					.resourceUrl(resourceUrl)
					.privateKey(privateKey)
					.keyPairId(properties.getCloudFrontKeyPairId())
					.expirationDate(Instant.now().plus(expiresIn))
					.build();

			SignedUrl signedUrl = cloudFrontUtilities.getSignedUrlWithCannedPolicy(request);
			return signedUrl.url();
		} catch (Exception ex) {
			throw new StorageExternalException("Failed to sign CloudFront URL for key " + storageKey, ex);
		}
	}

	private String normalizedDomain() {
		return properties.getCloudFrontDomain()
				.trim()
				.replaceFirst("^https?://", "")
				.replaceFirst("/+$", "");
	}

	/**
	 * Decodes a PKCS8 or PKCS1 PEM-encoded RSA private key into a {@link PrivateKey}.
	 *
	 * @param pem PEM text, including the {@code BEGIN/END ... PRIVATE KEY} header/footer
	 * @return the parsed private key
	 */
	PrivateKey parsePrivateKey(String pem) {
		try {
			byte[] decoded = decodePemBody(pem);
			if (pem.contains("BEGIN RSA PRIVATE KEY")) {
				return parsePkcs1PrivateKey(decoded);
			}
			return parsePkcs8PrivateKey(decoded);
		} catch (Exception ex) {
			throw new StorageExternalException("Failed to parse CloudFront private key. "
					+ "It must be a valid PKCS8 or PKCS1 RSA PEM private key.", ex);
		}
	}

	private byte[] decodePemBody(String pem) {
		String base64Body = pem
				.replace("\\n", "\n")
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replace("-----BEGIN RSA PRIVATE KEY-----", "")
				.replace("-----END RSA PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
		return Base64.getDecoder().decode(base64Body);
	}

	private PrivateKey parsePkcs8PrivateKey(byte[] decoded) throws Exception {
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
		return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
	}

	private PrivateKey parsePkcs1PrivateKey(byte[] decoded) throws Exception {
		DerReader sequence = new DerReader(decoded).readSequence();
		sequence.readInteger(); // version
		BigInteger modulus = sequence.readInteger();
		BigInteger publicExponent = sequence.readInteger();
		BigInteger privateExponent = sequence.readInteger();
		BigInteger primeP = sequence.readInteger();
		BigInteger primeQ = sequence.readInteger();
		BigInteger primeExponentP = sequence.readInteger();
		BigInteger primeExponentQ = sequence.readInteger();
		BigInteger crtCoefficient = sequence.readInteger();

		RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(
				modulus,
				publicExponent,
				privateExponent,
				primeP,
				primeQ,
				primeExponentP,
				primeExponentQ,
				crtCoefficient);
		return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
	}

	private static final class DerReader {

		private static final int SEQUENCE_TAG = 0x30;
		private static final int INTEGER_TAG = 0x02;

		private final byte[] data;
		private int offset;

		private DerReader(byte[] data) {
			this.data = data;
		}

		private DerReader readSequence() {
			expectTag(SEQUENCE_TAG);
			int length = readLength();
			byte[] sequenceBytes = Arrays.copyOfRange(data, offset, offset + length);
			offset += length;
			return new DerReader(sequenceBytes);
		}

		private BigInteger readInteger() {
			expectTag(INTEGER_TAG);
			int length = readLength();
			byte[] integerBytes = Arrays.copyOfRange(data, offset, offset + length);
			offset += length;
			return new BigInteger(1, integerBytes);
		}

		private void expectTag(int expectedTag) {
			if (offset >= data.length || (data[offset++] & 0xff) != expectedTag) {
				throw new IllegalArgumentException("Invalid DER structure.");
			}
		}

		private int readLength() {
			int firstByte = data[offset++] & 0xff;
			if ((firstByte & 0x80) == 0) {
				return firstByte;
			}
			int bytesCount = firstByte & 0x7f;
			int length = 0;
			for (int i = 0; i < bytesCount; i++) {
				length = (length << 8) + (data[offset++] & 0xff);
			}
			return length;
		}
	}
}

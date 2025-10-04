package semika.skillshared.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import semika.skillshared.model.response.AppleKey;
import semika.skillshared.model.response.AppleKeysResponse;
import semika.skillshared.model.response.AppleUser;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;

@Service
public class AppleKeyService {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";

    public String extractKeyIdFromToken(String idToken) {
        try {
            // Split the JWT token into its three parts
            String[] chunks = idToken.split("\\.");
            Base64.Decoder decoder = Base64.getUrlDecoder();

            // Decode the header (first part)
            String header = new String(decoder.decode(chunks[0]));

            // Parse header JSON to extract kid
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> jsonHeader = objectMapper.readValue(header, new TypeReference<Map<String, Object>>() {});

            // Return the Key ID
            return (String) jsonHeader.get("kid");

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract Key ID from token", e);
        }
    }

    public AppleKey getAppleKey(String keyId) {
        try {
            // Fetch keys from Apple
            //Create HttpClient
            HttpClient client = HttpClient.newHttpClient();

            // Build POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(APPLE_KEYS_URL)) // test endpoint
                    .GET()
                    .build();

            // Send request and get response
            HttpResponse<String> response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response and find matching key
            ObjectMapper mapper = new ObjectMapper();
            AppleKeysResponse keysResponse = mapper.readValue(response.body().toString(), AppleKeysResponse.class);

            return keysResponse.getKeys().stream()
                    .filter(key -> keyId.equals(key.getKid()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Apple key not found for kid: " + keyId));

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Apple keys", e);
        }
    }

    public PublicKey generatePublicKey(AppleKey keyDetails) {
        try {
            // Decode the modulus and exponent from Base64
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(keyDetails.getN()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(keyDetails.getE()));

            // Create RSA public key specification
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);

            // Generate the public key
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = factory.generatePublic(spec);

            return publicKey;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA public key", e);
        }
    }

    public AppleUser validateTokenAndExtractUser(String idToken, PublicKey publicKey) {
        try {
            // Parse and validate the JWT token
            Claims claims = Jwts.parser()
                    .setSigningKey(publicKey)
                    .build().parseSignedClaims(idToken)
                    .getBody();

            // Extract user information
            String appleId = claims.getSubject(); // Apple's unique user identifier
            String email = claims.get("email", String.class);
            Boolean emailVerified = claims.get("email_verified", Boolean.class);

            // Check if user exists in your database
            return new AppleUser(appleId, email, emailVerified);

        } catch (Exception e) {
            throw new RuntimeException("Token validation failed", e);
        }
    }
}

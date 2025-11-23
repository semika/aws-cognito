package semika.skillshared.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import semika.skillshared.config.AppleConfigProperties;
import semika.skillshared.config.GoogleConfigProperties;
import semika.skillshared.model.request.*;
import semika.skillshared.model.response.*;
import semika.skillshared.service.AppleKeyService;
import semika.skillshared.service.UserSignupService;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@EnableConfigurationProperties(GoogleConfigProperties.class)
@Slf4j
public class AuthController {

    @Autowired
    private UserSignupService userSignupService;

    @Autowired
    private GoogleConfigProperties googleConfigProperties;

    @Autowired
    private AppleConfigProperties appleConfigProperties;

    @Autowired
    private AppleKeyService appleKeyService;

    @PostMapping("/createPool")
    public ResponseEntity<SignupResponse> createPool(@RequestBody MosaicSignupRequest mosaicSignupRequest) {
        SignupResponse response = userSignupService.createPool(mosaicSignupRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/msg")
    public ResponseEntity<SignupResponse> message() {
        SignupResponse response = SignupResponse.builder().message("succes").build();
        return new ResponseEntity(response, HttpStatus.OK);
    }

    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(@RequestBody UserDto userDto) {
        String message = userSignupService.createNewUser(userDto);
        return new ResponseEntity<String>(message, HttpStatus.OK);
    }
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@RequestBody MosaicSignupRequest mosaicSignupRequest) {
        SignUpResponse signUpResponse = userSignupService.signup(mosaicSignupRequest);
        return new ResponseEntity<SignUpResponse>(signUpResponse, HttpStatus.OK);
    }

    @PostMapping("/confirmSignup")
    public ResponseEntity<ConfirmSignUpResponse> confirmSignup(@RequestBody MosaicSingupConfirmRequest mosaicSingupConfirmRequest)
            throws NoSuchAlgorithmException, InvalidKeyException {
        ConfirmSignUpResponse response = userSignupService.confirmSignup(mosaicSingupConfirmRequest);
        return new ResponseEntity<ConfirmSignUpResponse>(response, HttpStatus.OK);
    }

    @PostMapping("/resendCode")
    public ResponseEntity<String> resendConfirmationCode(
            @RequestBody MosaicResendConfirmationCodeRequest mosaicResendConfirmationCodeRequest) {
        String response = userSignupService.resendConfirmationCode(mosaicResendConfirmationCodeRequest);
        return new ResponseEntity<String>(response, HttpStatus.OK);
    }

    @PostMapping("/sms")
    public ResponseEntity<String> sendSms(@RequestParam String phoneNumber) {
        userSignupService.sendSms("+447700185127");
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

    /*
      Exchange auth code given by Google to get access token.

     */
    @CrossOrigin(origins = {"http://localhost:3000", "https://supersacred-nonpersonally-danna.ngrok-free.dev"})
    @PostMapping("/register-user")
    public ResponseEntity<String> exchangeGoogleCode(
            @RequestBody UserRegistrationRequest userRegistrationRequest) throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException {

        log.info("Registering user " + userRegistrationRequest.toString());
        return new ResponseEntity<>("User Registration Success", HttpStatus.OK);
    }

    private OAuth2AuthorizeResponse exchangeGoogleAuthorizationToken(String code) throws IOException, InterruptedException {
        log.info("Invoking end point " + googleConfigProperties.getTokenEndPoint()
                +" with code " + code);

        SocialLoginTokenRequest socialLoginTokenRequest = new SocialLoginTokenRequest();

        socialLoginTokenRequest.setCode(code);
        socialLoginTokenRequest.setClientId(googleConfigProperties.getClientId());
        socialLoginTokenRequest.setClientSecret(googleConfigProperties.getClientSecret());
        socialLoginTokenRequest.setRedirectUri(googleConfigProperties.getRedirectUri());
        socialLoginTokenRequest.setGrantType(googleConfigProperties.getGrantType());

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(socialLoginTokenRequest);
        log.info("Making a request to google token end point with data \n\n" + json);

        //Make HttpClient request to google "/token" end point
        //Create HttpClient
        HttpClient client = HttpClient.newHttpClient();

        // Build POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(googleConfigProperties.getTokenEndPoint())) // test endpoint
                .header("Content-Type", "application/json")
                .header("User-Agent", "My App")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // Send request and get response
        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Print response
        System.out.println("Status code: " + response.statusCode());
        System.out.println("Response body: " + response.body());
        return mapper.readValue(response.body(), OAuth2AuthorizeResponse.class);
    }

    private OAuth2AuthorizeResponse exchangeAppleAuthorizationCode(String code) throws IOException, InterruptedException {

        String clientSecret = generateClientSecret(appleConfigProperties.getTeamId(),
                appleConfigProperties.getClientId(), appleConfigProperties.getSigninKeyId());

        String url = appleConfigProperties.getTokenEndPoint();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", appleConfigProperties.getClientId());
        parameters.put("code", code);
        parameters.put("client_secret", clientSecret);
        parameters.put("grant_type", appleConfigProperties.getGrantType());
        parameters.put("redirect_uri", appleConfigProperties.getRedirectUri());

        String form = parameters.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        OAuth2AuthorizeResponse aAuth2AuthorizeResponse = mapper.readValue(response.body().toString(), OAuth2AuthorizeResponse.class);

        return aAuth2AuthorizeResponse;
    }

    private String generateClientSecret(String teamId, String clientId, String keyId) {
        try {
            String privateKeyPath = "/Users/aeturnum/workspace/aws-cognito/src/main/resources/AuthKey_Y7P3956Z8Z.p8";

            String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", ""); // Remove whitespace
            byte[] pkcs8EncodedBytes = Base64.getDecoder().decode(privateKeyContent);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(keySpec);

            Instant now = Instant.now();

            return Jwts.builder()
                    .setHeaderParam("kid", keyId) // Key id
                    .setIssuer(teamId)           // Apple Developer Team ID
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(now.plusSeconds(1800))) // 30 minutes
                    .setAudience("https://appleid.apple.com")
                    .setSubject(clientId)        // Your client_id (Service ID)
                    .signWith(privateKey, SignatureAlgorithm.ES256)
                    .compact();

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Apple client secret", e);
        }
    }

//    private String generateClientSecret1(String teamId, String clientId, String keyId) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
//
//        String privateKeyFilePath = "/Users/aeturnum/workspace/aws-cognito/src/main/resources/AuthKey_Y7P3956Z8Z.p8";
//        // Read the private key from the .p8 file
//        String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyFilePath)))
//                .replace("-----BEGIN PRIVATE KEY-----", "")
//                .replace("-----END PRIVATE KEY-----", "")
//                .replaceAll("\\s", ""); // Remove whitespace
//
//        byte[] decodedKey = Base64.getDecoder().decode(privateKeyContent);
//
//        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
//        KeyFactory keyFactory = KeyFactory.getInstance("EC"); // Use EC for Elliptic Curve
//        ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);
//
//        // Create the JWT
//        long now = System.currentTimeMillis();
//        return JWT.create()
//                .withIssuer(teamId) // Your Apple Developer Team ID
//                .withIssuedAt(new Date(now))
//                .withExpiresAt(new Date(now + 3600 * 1000)) // Token valid for 1 hour (3600 seconds)
//                .withAudience("https://appleid.apple.com")
//                .withSubject("mosaic.green.apple.login.ai") // Your Service ID (e.g., com.example.service)
//                .withKeyId(keyId) // The Key ID (Kid) from your .p8 file
//                .sign(Algorithm.ECDSA256(privateKey)); // Use E
//    }

//    private String generateClientSecret2(String teamId, String clientId, String keyId) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
//
//        String privateKeyFilePath = "/Users/aeturnum/workspace/aws-cognito/src/main/resources/AuthKey_Y7P3956Z8Z.p8";
//        // Read the private key from the .p8 file
//        String privateKeyContent = new String(Files.readAllBytes(Paths.get(privateKeyFilePath)))
//                .replace("-----BEGIN PRIVATE KEY-----", "")
//                .replace("-----END PRIVATE KEY-----", "")
//                .replaceAll("\\s", ""); // Remove whitespace
//
//        byte[] decodedKey = Base64.getDecoder().decode(privateKeyContent);
//
//        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
//        KeyFactory keyFactory = KeyFactory.getInstance("EC"); // Use EC for Elliptic Curve
//        ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(keySpec);
//
//        // Set header
//        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
//                .keyID(keyId)
//                .build();
//
//        // Set claims
//        JWTClaimsSet claims = new JWTClaimsSet.Builder()
//                .issuer(teamId)
//                .issueTime(new Date())
//                .expirationTime(Date.from(Instant.now().plusSeconds(300))) // 5 min validity
//                .audience("https://appleid.apple.com")
//                .subject(clientId)
//                .build();
//
//        // Sign
//        SignedJWT signedJWT = new SignedJWT(header, claims);
//        signedJWT.sign(new ECDSASigner(privateKey));
//
//        return signedJWT.serialize();
//    }

    @PostMapping("/login-with-custom-challenge")
    public ResponseEntity<String> loginWithCustomChallenge(@RequestParam String email) {
        String result =  userSignupService.loginWithCustomChallenge(email);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/response-to-auth-challange")
    public ResponseEntity responseToAuthChallage(@RequestBody CognitoChallangeResponse cognitoChallangeResponse) {
        String result = userSignupService.responseToAuthChallage(cognitoChallangeResponse.getEmail(),
                cognitoChallangeResponse.getAnswer(),
                cognitoChallangeResponse.getSession());
        return new ResponseEntity(result, HttpStatus.OK);
    }

    @PostMapping("/google-authoriz-callback")
    public ResponseEntity<String> googleAuthorizedCallback(@RequestParam(value = "code", required = false) String code) throws IOException, InterruptedException, GeneralSecurityException, URISyntaxException {

        OAuth2AuthorizeResponse OAuth2AuthorizeResponse = exchangeGoogleAuthorizationToken(code);
        log.info("Google authorizer response : " + OAuth2AuthorizeResponse.toString());
        //extract "id_token" here and get the basic user profile details like firstName, lastName, email
        // and return to front end URL by appending these user details as query parameters

        String idTokenString = OAuth2AuthorizeResponse.getIdToken();  // token from frontend

        // JSON and HTTP factories
        JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        NetHttpTransport transport = new NetHttpTransport();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                // Specify the WEB_CLIENT_ID of the app that accesses the backend:
                .setAudience(Collections.singletonList(googleConfigProperties.getClientId()))
                // Or, if multiple clients access the backend:
                //.setAudience(Arrays.asList(WEB_CLIENT_ID_1, WEB_CLIENT_ID_2, WEB_CLIENT_ID_3))
                .build();

        // Verify the token
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();

            // Extract information from payload
            String userSub = payload.getSubject();  // Google's unique user ID
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            String firstName = (String) payload.get("given_name");;
            String lastName = (String) payload.get("family_name");

            URI uri = new URIBuilder(googleConfigProperties.getFrontEndRedirectUrl())
                    .addParameter("sub", userSub)
                    .addParameter("firstName", firstName)
                    .addParameter("lastName", lastName)
                    .addParameter("email", email)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", uri.toString());
            // Return a 302 redirect with the Location header
            return ResponseEntity.status(HttpStatus.FOUND)
                    .headers(headers)
                    .build();
        } else {
            System.out.println("Invalid ID token.");
            URI uri = new URIBuilder(googleConfigProperties.getFrontEndRedirectUrl())
                    .addParameter("error", "Invalid Id Token")
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", uri.toString());
            // Return a 302 redirect with the Location header
            return ResponseEntity.status(HttpStatus.FOUND)
                    .headers(headers)
                    .build();
        }
    }

    @PostMapping("/apple-authoriz-callback")
    public ResponseEntity<String> appleAuthorizedCallback(@RequestParam(value = "code", required = false) String code,
                                                          @RequestParam(value = "user", required = false) String userJson)
            throws IOException, InterruptedException, GeneralSecurityException, URISyntaxException {

        OAuth2AuthorizeResponse oAuth2AuthorizeResponse = exchangeAppleAuthorizationCode(code);
        log.info("Apple authorizer response : " + oAuth2AuthorizeResponse.toString());
        // extract "id_token" here and get the basic user profile details like firstName, lastName, email
        // and return to front end URL by appending these user details as query parameters

        String keyId = appleKeyService.extractKeyIdFromToken(oAuth2AuthorizeResponse.getIdToken());
        AppleKey appleKey = appleKeyService.getAppleKey(keyId);
        PublicKey publicKey = appleKeyService.generatePublicKey(appleKey);
        AppleUser appleUser = appleKeyService.validateTokenAndExtractUser(oAuth2AuthorizeResponse.getIdToken(), publicKey);

        ObjectMapper mapper = new ObjectMapper();
        HttpHeaders headers = new HttpHeaders();

        if (userJson != null) {
            AppleAuthorizeUserResponse appleAuthorizeUserResponse = mapper.readValue(userJson, AppleAuthorizeUserResponse.class);
            URI uri = new URIBuilder(appleConfigProperties.getFrontEndRedirectUrl())
                    .addParameter("sub", appleUser.getAppleId())
                    .addParameter("firstName", appleAuthorizeUserResponse.getName().getFirstName())
                    .addParameter("lastName", appleAuthorizeUserResponse.getName().getLastName())
                    .addParameter("email", appleUser.getEmail())
                    .build();
            headers.add("Location", uri.toString());
            // Return a 302 redirect with the Location header
            return ResponseEntity.status(HttpStatus.FOUND)
                    .headers(headers)
                    .build();
        } else {
            URI uri = new URIBuilder(appleConfigProperties.getFrontEndRedirectUrl())
                    .addParameter("sub", appleUser.getAppleId())
                    .addParameter("email", appleUser.getEmail())
                    .build();
            headers.add("Location", uri.toString());
            // Return a 302 redirect with the Location header
            return ResponseEntity.status(HttpStatus.FOUND)
                    .headers(headers)
                    .build();
        }
    }
}

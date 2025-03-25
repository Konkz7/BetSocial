package com.example.World.External.Payments;

import com.example.World.Users.User_;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.Http;
import com.google.api.client.util.Value;
import jakarta.servlet.http.HttpSession;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Cipher;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import org.json.JSONObject;


@Service
public class CircleService {

    private final String CIRCLE_SAND_API_KEY = "SAND_API_KEY:c48b65f1e716e79d00a0a20234c5f96d:c2aed480893c08190860dbfe35209e2f";
    private final String CIRCLE_TEST_API_KEY = "TEST_API_KEY:bc7f42a24c8213412f511d669c3555fe:d94e7dd4225e001bfd52c7178b54c66c";
    private final String API_KEY = CIRCLE_TEST_API_KEY;

    private final String CIRCLE_PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEArnU/ubU2qFIIc7ao9aeBlYc+wfdRAh4PZSrTb5+HPLmL63PpyGEq7myL/pYtGyRyLMYNkgGG+wJBOlN4SpAM3Jct9XDwYaFuX5r517mFNThqqADt8zOQNDJVG+Mejc8yTGpQFibmAF9143NLX2PfjXA8qPinTfVTBe/SOuc0oVTBk7W95h9CC3dxyNKNDaYjwosurATNie2ahbBvb1/TPHEvx+Ol8lSL8ektTJ4WC2WsK+iceWIpwyq8ZdiW2CrS1/eZ5cYn1NtPMVAaLAU5K8TmqcFDhW+Ucbdc9yZxWzLAsgiz0GH7aKXPc+Ft95v4CMGN2i/LTSNSYq1JW/zQvFOpYYzhM682GsnH2ymdcL1BNOegEZ7R3lUGroDDOiybdb90Src3lrVK07x5T12Pj5NXoX8XNPWOjQEUonchl8Hzd6EYihMOqPioQ+5t664/efC7ASiU1Ww7UGaS3gT3yqB1ihorycOJ7vRulDrG1vr744Ulq9RiXnx4SJ4aG17TgkEat3nyGzxGp4OKVb+xZlB2vYmTgDPHIfbrBMEwUX3TC/w/oMUCH1K1TsepH03wJFOY2ewlk1ldn9kL6KUnYpH96jRMuf+PZJfgFLXGXlMYZp9yE1y86bLhVapUpBIanw3XSsH1XwHLEcPB/O5i4x7rUNVc1/e3KITaTNRLrQUCAwEAAQ==";
    private String ENCRYPTED_ENTITY_SECRET;

    @Autowired
    private Environment env;

    CircleService(Environment env) {
        Security.addProvider(new BouncyCastleProvider());
    }

    public boolean generateEncryptedEntitySecret() throws Exception {
        // Encrypt the entity secret

        String encryptedBase64 = encryptWithRSA(CIRCLE_PUBLIC_KEY, env.getProperty("CIRCLE_SECRET"));

        // Verify the length of the Base64 ciphertext
        if (encryptedBase64.length() == 684) {
            System.out.println("Encryption successful! Encrypted secret: \n" + encryptedBase64);
            ENCRYPTED_ENTITY_SECRET = encryptedBase64;
            return true;
        } else {
            System.out.println("Error: Ciphertext length is " + encryptedBase64.length() + " instead of 684.");
            return false;
        }
    }


    public String encryptWithRSA(String publicKeyBase64, String hexEncodedEntitySecret) throws Exception {
        // Convert hex-encoded secret to bytes
        byte[] entitySecret = hexStringToByteArray(hexEncodedEntitySecret);

        // Ensure entity secret is exactly 32 bytes
        if (entitySecret.length != 32) {
            throw new IllegalArgumentException("Invalid entity secret: Must be 32 bytes (64 hex characters)");
        }

        // Decode the Base64 public key and create PublicKey object
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        // Encrypt data using RSA with OAEP and SHA-256
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedData = cipher.doFinal(entitySecret);

        // Convert encrypted data to Base64 and return
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    // Utility method to convert hex string to byte array
    private byte[] hexStringToByteArray(String hex) {
        int length = hex.length();
        System.out.print("Length: " + length + "\n");
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }


    public HttpResponse<String> getPublicKey() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.circle.com/v1/w3s/config/entity/publicKey"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.print(response.body());
        return response;
    }

    public HttpResponse<String> createWalletSet(String idemKey, String setName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.circle.com/v1/w3s/developer/walletSets"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("POST", HttpRequest.BodyPublishers.ofString("{\"idempotencyKey\":\"" + idemKey + "\"," +
                        "\"entitySecretCipherText\":\"" + ENCRYPTED_ENTITY_SECRET + "\",\"name\":\"" + setName + "\"}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return response;
    }

    public HttpResponse<String> createWallets(String idemKey, String walletSetId, int count) throws IOException, InterruptedException {
        String blockChain = "SOL-DEVNET";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.circle.com/v1/w3s/developer/wallets"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("POST", HttpRequest.BodyPublishers.ofString("{\"idempotencyKey\":\"" + idemKey + "\"," +
                        "\"entitySecretCipherText\":\"" + ENCRYPTED_ENTITY_SECRET + "\",\"blockchains\":[\"" + blockChain + "\"]," +
                        "\"count\":" + count + ",\"walletSetId\":\"" + walletSetId + "\"}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return response;
    }

    public HttpResponse<String> getWallet(String walletId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.circle.com/v1/w3s/wallets/" + walletId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return response;
    }

    public HttpResponse<String> getBalance(String walletId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.circle.com/v1/w3s/wallets/" + walletId + "/balances"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return response;
    }

    public HttpResponse<String> initiateTransaction(String idemKey, String tokenId, Float amount, String walletId) throws IOException, InterruptedException {
        String feeLevel = "MEDIUM";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.circle.com/v1/w3s/developer/transactions/transfer"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .method("POST", HttpRequest.BodyPublishers.ofString("{\"idempotencyKey\":\"" + idemKey + "\"" +
                        ",\"entitySecretCipherText\":\"" + ENCRYPTED_ENTITY_SECRET + "\",\"amounts\":[\"" + amount + "\"]," +
                        "\"feeLevel\":\"" + feeLevel + "\",\"tokenId\":\"" + tokenId + "\"," +
                        "\"walletId\":\"" + walletId + "\"}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return response;
    }

    public HttpResponse<String> addCard(String idemKey, User_ user, String sessionId, String ipAddress,
                        BillingDTO billing, String encryptedData) throws IOException, InterruptedException {

        JSONObject requestBody = new JSONObject();
        requestBody.put("idempotencyKey", idemKey);
        requestBody.put("encryptedData", encryptedData);

        JSONObject billingDetails = new JSONObject();
        billingDetails.put("name", billing.fullName());
        billingDetails.put("city", billing.city());
        billingDetails.put("country", billing.country());
        billingDetails.put("line1", billing.line1());
        billingDetails.put("line2", billing.line2());
        billingDetails.put("postalCode", billing.postalCode());
        requestBody.put("billingDetails", billingDetails);

        requestBody.put("expMonth", billing.expMonth());  // Ensure it's a string if needed
        requestBody.put("expYear", billing.expYear());

        JSONObject metadata = new JSONObject();
        metadata.put("email", user.email());
        metadata.put("phoneNumber", user.phone_number());
        metadata.put("sessionId", sessionId);
        metadata.put("ipAddress", ipAddress);
        requestBody.put("metadata", metadata);

        // Convert to String
        String requestBodyString = requestBody.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api-sandbox.circle.com/v1/cards"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + CIRCLE_SAND_API_KEY)
                .method("POST", HttpRequest.BodyPublishers.ofString(requestBodyString))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return response;
    }


    public HttpResponse<String> makePayment(String idemKey, User_ user, String cardId, Float amount, String sessionId,
                                            String ipAddress) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api-sandbox.circle.com/v1/payments"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + CIRCLE_SAND_API_KEY)
                .method("POST", HttpRequest.BodyPublishers.ofString("{" +
                        "\"idempotencyKey\":\"" + idemKey + "\"," +
                        "\"metadata\":{\"email\":\"" + user.email() + "\",\"phoneNumber\":\"" + user.phone_number() + "\"," +
                        "\"sessionId\":\"" + sessionId + "\",\"ipAddress\":\"" + ipAddress + "\"}," +
                        "\"amount\":{\"amount\":\"" + amount + "\",\"currency\":\"USD\"},\"verification\":\"cvv\"," +
                        "\"source\":{\"id\":\"" + cardId + "\",\"type\":\"card\"}}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return response;
    }

    public HttpResponse<String> getCard(String cardId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api-sandbox.circle.com/v1/cards/"+cardId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + CIRCLE_SAND_API_KEY)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        return response;
    }



    public String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing session ID", e);
        }
    }

    public String extractId(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            return rootNode.path("data").path("id").asText();  // Extract "id"
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}

package com.example.World.External.Payments;

import com.example.World.Cards.CardRepository;
import com.example.World.Cards.Card_;
import com.example.World.Users.UserRepository;
import com.example.World.Users.User_;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RequestMapping("/circle")
@RestController
public class CircleController {

    private final CircleService circleService;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final String USERS_WALLET_ID = "77397fcc-793e-553f-8b68-f93630488cb3";


    public CircleController(CircleService circleService, UserRepository userRepository, CardRepository cardRepository) {
        this.circleService = circleService;
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }


    @GetMapping("/get-secret")
    ResponseEntity<String> getSecret() throws Exception {
        if(circleService.generateEncryptedEntitySecret()) {
            return ResponseEntity.ok("Everythings fine!");
        }else{
            return ResponseEntity.badRequest().body("Hell on earth!");
        }
    }

    @GetMapping("/get-public-key")
    ResponseEntity<String> getPublicKey() throws Exception {
        HttpResponse<String> response = circleService.getPublicKey();

        if(response.statusCode() == 200) {
            return ResponseEntity.ok("Public key retrieved successfully!\n" + response.body());
        }else{
            return ResponseEntity.badRequest().body("Failed to retrieve public key!");
        }
    }


    @PostMapping("/create-wallet-set")
    ResponseEntity<String> createWalletSet(@RequestParam String name) throws Exception {

        HttpResponse<String> response = circleService.createWalletSet(UUID.randomUUID().toString(),name);

        if(response.statusCode() == 200 || response.statusCode() == 201) {
            return ResponseEntity.ok("Wallet set created successfully!\n" + response.body());
        }else{
            return ResponseEntity.badRequest().body("Failed to create wallet set!\n" + response.body());
        }
    }



    @PostMapping("/create-user-wallet")
    ResponseEntity<String> createWallets(HttpSession session) throws Exception {
        HttpResponse<String> response = circleService.createWallets(UUID.randomUUID().toString(), USERS_WALLET_ID, 1);
        Long uid = (Long) session.getAttribute("userId");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonResponse = objectMapper.readTree(response.body());

        // Extract the wallet address from the response
        String walletAddress = jsonResponse.path("data").path("wallets").get(0).path("id").asText();

        if(response.statusCode() == 200 || response.statusCode() == 201) {
            userRepository.setWalletAddress(uid, walletAddress);
            return ResponseEntity.ok("Wallet created successfully!\nAddress: " + walletAddress);
        } else {
            return ResponseEntity.badRequest().body("Failed to create wallet(s)!\n" + response.body());
        }
    }

    @GetMapping("/get-user-wallet")
    ResponseEntity<String> getUserWallet(HttpSession session) throws IOException, InterruptedException {

        Long uid = (Long) session.getAttribute("userId");
        User_ user = userRepository.findById(uid).orElseThrow();

        HttpResponse<String> response = circleService.getWallet(user.wallet_address());

        if(response.statusCode() == 200 || response.statusCode() == 201) {
            return ResponseEntity.ok(response.body());
        } else {
            return ResponseEntity.badRequest().body("Failed to get wallet!\n" + response.body());
        }

    }

    @GetMapping("/get-balance")
    ResponseEntity<String> getBalance(HttpSession session) throws IOException, InterruptedException {

        Long uid = (Long) session.getAttribute("userId");
        User_ user = userRepository.findById(uid).orElseThrow();

        HttpResponse<String> response = circleService.getBalance(user.wallet_address());

        if(response.statusCode() == 200 || response.statusCode() == 201) {
            return ResponseEntity.ok(response.body());
        } else {
            return ResponseEntity.badRequest().body("Failed to get Balance!\n" + response.body());
        }

    }

    @PostMapping("/create-card")
    public ResponseEntity<String> createCard(HttpSession session,
                                             @RequestParam String encryptedData,
                                             @RequestParam String ipAddress,
                                             @RequestBody BillingDTO billing) {

        Long uid  = (Long) session.getAttribute("userId");

        try {
            String idempotencyKey = UUID.randomUUID().toString(); // Generate a unique key
            User_ user = userRepository.findById(uid).orElseThrow(); // Fetch the authenticated user

            HttpResponse<String> response = circleService.addCard(idempotencyKey, user, circleService.hash(session.getId()),
                    ipAddress, billing, encryptedData);


            cardRepository.save(new Card_(circleService.extractId(response.body()), uid , new Date().getTime()));

            return ResponseEntity.status(response.statusCode()).body(response.body());
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body("Error processing request: " + e.getMessage());
        }
    }


}

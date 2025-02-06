package com.example.World.External.Payments;

import com.google.api.client.util.Value;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.secret.key}")
    private String secretKey;

    public String createPaymentIntent(Long amount, String currency) throws StripeException {
        Stripe.apiKey = secretKey;

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount * 100L) // Convert to cents
                .setCurrency(currency)
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        return intent.getClientSecret();
    }
    /*
    @PostMapping("/create")
    public ResponseEntity<String> createPayment(@RequestParam Long amount, @RequestParam String currency) {
        try {
            String clientSecret = paymentService.createPaymentIntent(amount, currency);
            return ResponseEntity.ok(clientSecret);
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body("Payment failed: " + e.getMessage());
        }
    }

     */
}

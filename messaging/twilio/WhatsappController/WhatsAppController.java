package twilio.messaging.twilio.WhatsappController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import twilio.messaging.twilio.service.TwilioService;
import twilio.messaging.twilio.service.TwilioService.MessageResult;

import java.util.Map;

/**
 * REST Controller for handling WhatsApp and SMS messaging via Twilio API.
 * Uses TwilioService which retrieves credentials from Vault.
 */
@RestController
@RequestMapping("/api")
public class WhatsAppController {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppController.class);
    
    private final TwilioService twilioService;
    
    @Autowired
    public WhatsAppController(TwilioService twilioService) {
        this.twilioService = twilioService;
        log.info("WhatsAppController initialized with TwilioService");
    }
    
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody Map<String, String> body) {
        try {
            // parse Json body data from html to a map
            String toRaw = str(body.get("to")).trim();
            String text = str(body.get("message")).trim();
            String ch = str(body.get("channel")).trim().toLowerCase();
            String channel = ch.isEmpty() ? "whatsapp" : ch;
            
            if (toRaw.isEmpty() || text.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", "'to' and 'message' are required"
                ));
            }
            
            // Use the TwilioService to send the message
            MessageResult result = twilioService.sendMessage(toRaw, text, channel);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "sid", result.getSid(),
                    "status", result.getStatus()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "ok", false,
                    "error", result.getError(),
                    "code", result.getCode()
                ));
            }
            
        } catch (Exception ex) {
            log.error("Unexpected error in send endpoint: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "ok", false,
                "error", ex.getMessage()
            ));
        }
    }
    
    @PostMapping("/send/whatsapp")
    public ResponseEntity<?> sendWhatsApp(@RequestBody Map<String, String> body) {
        try {
            String toRaw = str(body.get("to")).trim();
            String text = str(body.get("message")).trim();
            
            if (toRaw.isEmpty() || text.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", "'to' and 'message' are required"
                ));
            }
            
            MessageResult result = twilioService.sendWhatsAppMessage(toRaw, text);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "sid", result.getSid(),
                    "status", result.getStatus()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "ok", false,
                    "error", result.getError(),
                    "code", result.getCode()
                ));
            }
            
        } catch (Exception ex) {
            log.error("Unexpected error in WhatsApp send endpoint: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "ok", false,
                "error", ex.getMessage()
            ));
        }
    }
    
    @PostMapping("/send/sms")
    public ResponseEntity<?> sendSms(@RequestBody Map<String, String> body) {
        try {
            String toRaw = str(body.get("to")).trim();
            String text = str(body.get("message")).trim();
            
            if (toRaw.isEmpty() || text.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", "'to' and 'message' are required"
                ));
            }
            
            MessageResult result = twilioService.sendSmsMessage(toRaw, text);
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "sid", result.getSid(),
                    "status", result.getStatus()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "ok", false,
                    "error", result.getError(),
                    "code", result.getCode()
                ));
            }
            
        } catch (Exception ex) {
            log.error("Unexpected error in SMS send endpoint: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "ok", false,
                "error", ex.getMessage()
            ));
        }
    }
    
    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}

package twilio.messaging.twilio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Service class for handling Twilio messaging operations.
 * Uses Vault-retrieved credentials and provides a clean interface for messaging operations.
 */
@Service
public class TwilioService {
    private static final Logger log = LoggerFactory.getLogger(TwilioService.class);
    
    // WhatsApp sandbox sender (or replace with your approved WA sender)
    private static final String FROM_WHATSAPP = "whatsapp:+14155238886";
    
    // Optional: SMS via Messaging Service
    private static final String MESSAGING_SERVICE_SID = "MG6888ce1728921d9754dfb8c7b6208401";
    
    private final String accountSid;
    private final String authToken;
    
    @Autowired
    public TwilioService(@Qualifier("twilioAccountSid") String accountSid, 
                        @Qualifier("twilioAuthToken") String authToken) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        
        // Initialize Twilio with Vault-retrieved credentials
        Twilio.init(accountSid, authToken);
        log.info("TwilioService initialized with account SID: {}", accountSid);
    }
    
    /**
     * Send a message via Twilio
     * 
     * @param to The recipient number
     * @param message The message content
     * @param channel The channel to use ("sms" or "whatsapp")
     * @return MessageResult containing success status and message details
     */
    public MessageResult sendMessage(String to, String message, String channel) {
        try {
            String normalizedChannel = (channel == null || channel.trim().isEmpty()) ? "whatsapp" : channel.trim().toLowerCase();
            
            Message twilioMsg;
            
            if ("sms".equals(normalizedChannel)) {
                // Send SMS via Messaging Service
                String toSms = normalizeSmsE164(to);
                twilioMsg = Message.creator(new PhoneNumber(toSms), MESSAGING_SERVICE_SID, message).create();
                log.debug("SMS sent to {} via Messaging Service", toSms);
            } else {
                // Default: WhatsApp (sandbox or WA-enabled sender)
                String toWa = normalizeWhatsApp(to);
                twilioMsg = Message.creator(new PhoneNumber(toWa), new PhoneNumber(FROM_WHATSAPP), message).create();
                log.debug("WhatsApp message sent to {} from {}", toWa, FROM_WHATSAPP);
            }
            
            return new MessageResult(true, twilioMsg.getSid(), twilioMsg.getStatus().toString(), null, null);
            
        } catch (ApiException ex) {
            log.error("Twilio API exception sending message to {}: {}", to, ex.getMessage());
            return new MessageResult(false, null, null, ex.getMessage(), ex.getCode());
        } catch (Exception ex) {
            log.error("Unexpected error sending message to {}: {}", to, ex.getMessage(), ex);
            return new MessageResult(false, null, null, ex.getMessage(), null);
        }
    }
    
    /**
     * Send a WhatsApp message
     * 
     * @param to The recipient WhatsApp number
     * @param message The message content
     * @return MessageResult containing success status and message details
     */
    public MessageResult sendWhatsAppMessage(String to, String message) {
        return sendMessage(to, message, "whatsapp");
    }
    
    /**
     * Send an SMS message
     * 
     * @param to The recipient phone number
     * @param message The message content
     * @return MessageResult containing success status and message details
     */
    public MessageResult sendSmsMessage(String to, String message) {
        return sendMessage(to, message, "sms");
    }
    
    private static String normalizeWhatsApp(String v) {
        String s = v == null ? "" : v.trim();
        if (s.startsWith("whatsapp:")) return s;
        if (s.startsWith("+")) return "whatsapp:" + s;
        throw new IllegalArgumentException("WhatsApp numbers must start with '+' or 'whatsapp:'. Got: " + v);
    }
    
    private static String normalizeSmsE164(String v) {
        String s = v == null ? "" : v.trim();
        if (!s.startsWith("+")) {
            throw new IllegalArgumentException("SMS numbers must be E.164 like +447700900000. Got: " + v);
        }
        return s;
    }
    
    /**
     * Result class for message sending operations
     */
    public static class MessageResult {
        private final boolean success;
        private final String sid;
        private final String status;
        private final String error;
        private final String code;
        
        public MessageResult(boolean success, String sid, String status, String error, String code) {
            this.success = success;
            this.sid = sid;
            this.status = status;
            this.error = error;
            this.code = code;
        }
        
        public boolean isSuccess() { return success; }
        public String getSid() { return sid; }
        public String getStatus() { return status; }
        public String getError() { return error; }
        public String getCode() { return code; }
    }
}



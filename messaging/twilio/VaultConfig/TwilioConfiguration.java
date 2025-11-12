package twilio.messaging.twilio.VaultConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import com.twilio.Twilio;
import com.twilio.http.TwilioRestClient;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>This class provides the means through which the interface to the Twilio API is configured.</p>
 * <p>It retrieves Twilio credentials from HashiCorp Vault and provides them as Spring beans.</p>
 * 
 * @author Your Name
 */
@Configuration
public class TwilioConfiguration {
    private static final Logger log = LoggerFactory.getLogger(TwilioConfiguration.class);
    
    // Parameter names that we expect our information to be stored under in Vault
    private static final String ACCOUNT_SID = "account_sid";
    private static final String AUTH_TOKEN = "auth_token";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    
    @Autowired
    private VaultTemplate template;
    
    @Getter
    @Setter
    private class TwilioProperties {
        private String accountSid;
        private String authToken;
        private String username;
        private String password;
        
        public boolean isValid() {
            if (accountSid != null && authToken != null) {
                return true;
            }
            return false;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder("{TwilioProperties [accountSid: ");
            sb.append(accountSid);
            sb.append(", authToken: ");
            if (authToken == null) {
                sb.append("null");
            } else {
                sb.append("**not shown**");
            }
            sb.append(", username: ");
            sb.append(username);
            sb.append(", password: ");
            if (password == null) {
                sb.append("null");
            } else {
                sb.append("**not shown**");
            }
            sb.append("]}");
            return sb.toString();
        }
    }
    
    @Bean
    @Scope("singleton")
    @Primary
    public String twilioAccountSid() {
        TwilioProperties tprops = getConnectionProperties();
        return tprops.getAccountSid();
    }
    
    @Bean
    @Scope("singleton")
    @Primary
    public String twilioAuthToken() {
        TwilioProperties tprops = getConnectionProperties();
        return tprops.getAuthToken();
    }
    
    @Bean
    @Scope("singleton")
    @Primary
    public TwilioRestClient twilioRestClient() {
        TwilioProperties tprops = getConnectionProperties();
        
        try {
            // Initialize Twilio with credentials from Vault
            Twilio.init(tprops.getAccountSid(), tprops.getAuthToken());
            
            // Create and return the Twilio REST client
            TwilioRestClient client = new TwilioRestClient.Builder(tprops.getAccountSid(), tprops.getAuthToken())
                    .build();
            
            log.debug("Twilio client initialized successfully with account SID: {}", tprops.getAccountSid());
            return client;
            
        } catch (Exception e) {
            log.error("Failed to initialize Twilio client: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize Twilio client.");
        }
    }
    
    private TwilioProperties getConnectionProperties() {
        TwilioProperties tprops = new TwilioProperties();
        VaultKeyValueOperations kvo = null;
        VaultResponse response = null;
        String location = null;
        
        // Set up the location - using the same pattern as MongoDB but for Twilio
        location = "twilio";
        log.debug("Using Vault location: {}", location);
        
        try {
            kvo = template.opsForKeyValue("secret", KeyValueBackend.KV_1);
        } catch (Exception e) {
            log.error("Exception obtaining Vault operations instance: " + e.getMessage(), e);
            throw new IllegalStateException("Exception obtaining Vault operations instance: " + e.getMessage());
        }
        
        try {
            response = kvo.get(location);
        } catch (Exception e) {
            log.error("Exception retrieving Twilio connection parameters: " + e.getMessage(), e);
            throw new IllegalStateException("Exception retrieving Twilio connection parameters: " + e.getMessage());
        }
        
        if (response != null && response.getData() != null) {
            tprops.setAccountSid((String) response.getData().get(ACCOUNT_SID));
            tprops.setAuthToken((String) response.getData().get(AUTH_TOKEN));
            tprops.setUsername((String) response.getData().get(USERNAME));
            tprops.setPassword((String) response.getData().get(PASSWORD));
        } else {
            log.error("Failed to retrieve Twilio connection parameters from Vault.");
            throw new IllegalStateException("Failed to retrieve Twilio connection parameters from Vault.");
        }
        
        if (tprops.isValid()) {
            log.debug("Retrieved Twilio properties: {}", tprops);
            return tprops;
        }
        
        // If we're here, there's something missing.
        log.error("Retrieved Twilio connection properties incomplete: {}", tprops);
        throw new IllegalStateException("Incomplete Twilio connection properties found.");
    }
}

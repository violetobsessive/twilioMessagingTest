# Twilio Vault Integration

This project demonstrates how to integrate HashiCorp Vault with a Spring Boot application to securely manage Twilio API credentials, following the same pattern used in the codebase for MongoDB and ActiveMQ.

## Overview

The application uses Spring Vault to retrieve Twilio credentials (Account SID and Auth Token) from HashiCorp Vault instead of hardcoding them in the application. This provides better security and credential management.

## Architecture

### Components

1. **TwilioConfiguration** (`twilio/messaging/twilio/VaultConfig/TwilioConfiguration.java`)
   - Spring `@Configuration` class that retrieves Twilio credentials from Vault
   - Provides `TwilioRestClient` bean and credential beans (`twilioAccountSid`, `twilioAuthToken`)
   - Uses the same pattern as `MongoDatabaseConfiguration` in the Cashbook codebase

2. **TwilioService** (`twilio/messaging/twilio/service/TwilioService.java`)
   - Service layer that handles Twilio messaging operations
   - Uses injected Vault-retrieved credentials
   - Provides clean interface for sending WhatsApp and SMS messages

3. **WhatsAppController** (`twilio/messaging/twilio/WhatsappController/WhatsAppController.java`)
   - REST controller that exposes messaging endpoints
   - Uses `TwilioService` for actual messaging operations
   - No longer contains hardcoded credentials

## Vault Setup

### 1. Create the Twilio Secret in Vault

Using the Vault UI or CLI, create a secret at path `secret/twilio` with the following structure:

```json
{
  "account_sid": "AC5130fd1b9b078ca4",
  "auth_token": "d8a2759ea474cff0d6",
  "user": "marian@cashbook.com",
  "password": "Cashbook!"
}
```

### 2. Vault Configuration

The application is configured to connect to Vault using the following properties in `application.properties`:

```properties
# Vault Configuration
spring.cloud.vault.enabled=true
spring.cloud.vault.uri=https://localhost:8200
spring.cloud.vault.authentication=APPROLE
spring.cloud.vault.app-role.role-id=1a28fbae-1a3e-ebb3-ce10-833834658429
spring.cloud.vault.app-role.secret-id=0df80ef2-b59c-24df-a886-ce5fe8c1c499

# SSL Configuration for Vault
spring.cloud.vault.ssl.trust-store=classpath:cacert.pem
spring.cloud.vault.ssl.trust-store-type=pem

# Vault KV Configuration
spring.cloud.vault.kv.enabled=true
spring.cloud.vault.kv.backend=secret
spring.cloud.vault.kv.profile-separator=/
spring.cloud.vault.kv.default-context=twilio
```

## How It Works

### 1. Credential Retrieval

The `TwilioConfiguration` class:

```java
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
```

- Uses `VaultTemplate` to access the KV v1 backend at mount `secret`
- Retrieves the secret at path `twilio` (from `spring.cloud.vault.kv.default-context`)
- Extracts `account_sid` and `auth_token` from the secret data
- Provides them as Spring beans for injection

### 2. Service Layer

The `TwilioService` class:

```java
@Autowired
public TwilioService(@Qualifier("twilioAccountSid") String accountSid, 
                    @Qualifier("twilioAuthToken") String authToken) {
    this.accountSid = accountSid;
    this.authToken = authToken;
    
    // Initialize Twilio with Vault-retrieved credentials
    Twilio.init(accountSid, authToken);
    log.info("TwilioService initialized with account SID: {}", accountSid);
}
```

- Injects the Vault-retrieved credentials using `@Qualifier`
- Initializes the Twilio client with these credentials
- Provides methods for sending WhatsApp and SMS messages

### 3. Controller Layer

The `WhatsAppController` class:

```java
@Autowired
public WhatsAppController(TwilioService twilioService) {
    this.twilioService = twilioService;
    log.info("WhatsAppController initialized with TwilioService");
}
```

- Uses dependency injection to get the `TwilioService`
- No longer contains any hardcoded credentials
- Delegates all Twilio operations to the service layer

## API Endpoints

### Send Message (Generic)
```
POST /api/send
Content-Type: application/json

{
  "to": "+1234567890",
  "message": "Hello from Twilio!",
  "channel": "whatsapp"  // or "sms"
}
```

### Send WhatsApp Message
```
POST /api/send/whatsapp
Content-Type: application/json

{
  "to": "+1234567890",
  "message": "Hello from WhatsApp!"
}
```

### Send SMS Message
```
POST /api/send/sms
Content-Type: application/json

{
  "to": "+1234567890",
  "message": "Hello from SMS!"
}
```

## Response Format

### Success Response
```json
{
  "ok": true,
  "sid": "SM1234567890abcdef",
  "status": "queued"
}
```

### Error Response
```json
{
  "ok": false,
  "error": "Error message",
  "code": "21211"  // Twilio error code (if applicable)
}
```

## Security Benefits

1. **No Hardcoded Credentials**: Credentials are never stored in source code
2. **Centralized Management**: All credentials managed in one place (Vault)
3. **Access Control**: Vault provides fine-grained access control via AppRole
4. **Audit Trail**: Vault logs all credential access
5. **Credential Rotation**: Easy to rotate credentials without code changes

## Comparison with Cashbook Pattern

This implementation follows the exact same pattern used in the Cashbook codebase:

### MongoDB Pattern (Cashbook)
```java
// MongoDatabaseConfiguration.java
location = "database/mongodb/";
location = location + ((String) SysValues.getVal(SysValues.PROPERTY_RUNTIME));
kvo = template.opsForKeyValue("secret", KeyValueBackend.KV_1);
response = kvo.get(location);
```

### Twilio Pattern (This Project)
```java
// TwilioConfiguration.java
location = "twilio";
kvo = template.opsForKeyValue("secret", KeyValueBackend.KV_1);
response = kvo.get(location);
```

Both use:
- Same Vault mount (`secret`)
- Same KV version (`KV_1`)
- Same `VaultTemplate` injection
- Same error handling patterns
- Same bean provisioning approach

## Troubleshooting

### Common Issues

1. **Vault Connection Failed**
   - Check `spring.cloud.vault.uri` is correct
   - Verify SSL certificate is valid
   - Ensure AppRole credentials are correct

2. **Secret Not Found**
   - Verify secret exists at `secret/twilio`
   - Check AppRole has permission to read the secret
   - Ensure secret contains required fields (`account_sid`, `auth_token`)

3. **Twilio API Errors**
   - Verify Account SID and Auth Token are correct
   - Check Twilio account status and permissions
   - Ensure phone numbers are in correct format

### Debugging

Enable debug logging for Vault operations:

```properties
logging.level.org.springframework.vault=DEBUG
logging.level.twilio.messaging.twilio=DEBUG
```

## Dependencies

Ensure these dependencies are in your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>9.14.1</version>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```



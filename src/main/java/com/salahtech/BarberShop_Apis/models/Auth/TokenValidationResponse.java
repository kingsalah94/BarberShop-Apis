package com.salahtech.BarberShop_Apis.models.Auth;

public class TokenValidationResponse {
 private boolean valid;
    private long expiresInSeconds;

    public TokenValidationResponse(boolean valid, long expiresInSeconds) {
        this.valid = valid;
        this.expiresInSeconds = expiresInSeconds;
    }

    public boolean isValid() {
        return valid;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }
}
// This class is used to encapsulate the response for token validation.
// It contains two fields: `valid` which indicates whether the token is valid or not,
// and `expiresInSeconds` which indicates the number of seconds until the token expires.
// The constructor initializes these fields, and there are getter methods to access their values.
// The `valid` field is a boolean that indicates if the token is valid.
// The `expiresInSeconds` field is a long that indicates how many seconds are left before the token expires.
// This class is typically used in the context of validating JWT tokens in an authentication system.
// It provides a structured way to return the validation result to the client.
// The `valid` field is set to true if the token is valid, and false otherwise.
// The `expiresInSeconds` field is set to the number of seconds remaining until the token expires.
// This information can be useful for the client to determine if they need to refresh the token or take other actions based on the token's validity and expiration time.
// The class is simple and straightforward, making it easy to use in various parts of the application where token validation is required.
// It can be serialized to JSON or other formats for communication with clients or other services.
// The class does not contain any additional methods or logic, as its primary purpose is to serve as a data transfer object (DTO)
// for token validation responses.

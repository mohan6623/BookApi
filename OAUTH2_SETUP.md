# OAuth2 Configuration Summary

## Important: OAuth2 Flow vs REST API

Your project uses **both**:
- **REST API with `/api` prefix** for traditional JWT-based authentication (register, login, etc.)
- **OAuth2 flow** with Spring Security's default endpoints at **root level** (not `/api`)

---

## OAuth2 Endpoints (Root Level - NOT `/api`)

Spring Security creates these automatically:

### Authorization Flow:
1. **User clicks "Login with Google/GitHub"** on frontend
2. Frontend redirects to: `http://localhost:8080/oauth2/authorization/google` (or `/github`)
3. User logs in with OAuth2 provider
4. Provider redirects back to: `http://localhost:8080/login/oauth2/code/google`
5. Spring Security processes the callback
6. `OAuth2LoginSuccessHandler` is triggered
7. **Backend generates JWT token** and redirects frontend to:
   ```
   http://localhost:5173/oauth2/redirect?token=YOUR_JWT_TOKEN
   ```

---

## Configuration Details

### In `application.properties`:
```ini
# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8080/login/oauth2/code/google

# GitHub OAuth2
spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.redirect-uri=http://localhost:8080/login/oauth2/code/github
```

### In `SecurityConfig.java`:
```java
// Allow Spring Security's OAuth2 endpoints (NOT /api prefix)
.requestMatchers("/login/oauth2/code/**", "/oauth2/authorization/**").permitAll()

// OAuth2 Login config
.oauth2Login(oauth2 -> oauth2
    .loginPage("/login")
    .successHandler(oAuth2LoginSuccessHandler)  // Custom handler → generates JWT
    .failureUrl("/login?error=true")
)
```

### In `OAuth2LoginSuccessHandler.java`:
- Extracts user info from OAuth2 provider (Google/GitHub)
- Creates or finds user in database
- **Generates JWT token** using `JwtService`
- Redirects to frontend with token: `{frontendUrl}/oauth2/redirect?token={token}`

---

## Authentication Flow Comparison

| Method | Endpoint | Security |
|--------|----------|----------|
| **Traditional JWT** | `/api/login` | Send username + password → Get JWT |
| **OAuth2 Google** | `/oauth2/authorization/google` | OAuth2 provider login → Get JWT |
| **OAuth2 GitHub** | `/oauth2/authorization/github` | OAuth2 provider login → Get JWT |

---

## Frontend Integration

### Traditional Login:
```javascript
POST /api/login
{
  "username": "user@example.com",
  "password": "password123"
}

Response: {
  "token": "eyJhbGciOiJIUzI1NiIs...",
  ...
}
```

### OAuth2 Login:
```html
<a href="http://localhost:8080/oauth2/authorization/google">Login with Google</a>
<a href="http://localhost:8080/oauth2/authorization/github">Login with GitHub</a>
```

After successful OAuth2 login, backend redirects to:
```
http://localhost:5173/oauth2/redirect?token=eyJhbGciOiJIUzI1NiIs...
```

Your frontend should:
1. Extract token from URL query parameter
2. Store in localStorage/sessionStorage
3. Use for subsequent API requests: `Authorization: Bearer {token}`

---

## Important Notes

⚠️ **OAuth2 endpoints are NOT under `/api` prefix** - This is by design:
- Spring Security handles OAuth2 at root level (`/login/oauth2/code/*`)
- Your custom JWT endpoints ARE under `/api` (`/api/login`, `/api/register`)
- Both methods generate and return JWT tokens

✅ **Security Configuration allows:**
- `/api/register` - Register new user
- `/api/login` - Traditional JWT login
- `/login/oauth2/code/**` - OAuth2 callback (Google/GitHub)
- `/oauth2/authorization/**` - OAuth2 authorization initiation
- All public GET endpoints under `/api/books`, `/api/bookid/*`, etc.

---

## Registering Redirect URIs with OAuth2 Providers

### Google Console:
- Go to Google Cloud Console → OAuth 2.0 Client IDs
- Add Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`

### GitHub Settings:
- Go to GitHub Developer Settings → OAuth Apps
- Add Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`

---

## Testing OAuth2 Locally

1. Start backend: `mvn spring-boot:run` (port 8080)
2. Start frontend: `npm run dev` (port 5173)
3. Click "Login with Google" or "Login with GitHub"
4. After successful login, frontend receives JWT token
5. Use token for subsequent API requests to `/api/**` endpoints


# Security Implementation Documentation

## Overview
This document outlines the security implementation for the Book Shelf application, including JWT authentication, OAuth2 integration, and authorization controls.

## Table of Contents
1. [Security Features](#security-features)
2. [Authentication Methods](#authentication-methods)
3. [Authorization Model](#authorization-model)
4. [Public vs Protected Endpoints](#public-vs-protected-endpoints)
5. [OAuth2 Integration](#oauth2-integration)
6. [JWT Token Management](#jwt-token-management)
7. [Security Best Practices](#security-best-practices)
8. [Testing OAuth2 Flow](#testing-oauth2-flow)

---

## Security Features

### Implemented Security Measures
- ✅ JWT-based stateless authentication
- ✅ OAuth2 login (Google & GitHub)
- ✅ Role-based access control (RBAC)
- ✅ Password encryption (BCrypt with strength 12)
- ✅ Token expiration (30 minutes for access, 7 days for refresh)
- ✅ Role version tracking (token invalidation on role change)
- ✅ Ownership validation for comments
- ✅ Input validation and sanitization
- ✅ CORS support
- ✅ Public read-only endpoints for browsing

---

## Authentication Methods

### 1. Traditional Login (Username/Password + JWT)
**Endpoint:** `POST /login`
**Request Body:**
```json
{
  "username": "user@example.com",
  "password": "password123"
}
```
**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "user@example.com",
  "role": "USER"
}
```

### 2. OAuth2 Login (Google/GitHub)
**Endpoints:**
- Google: `GET /oauth2/authorization/google`
- GitHub: `GET /oauth2/authorization/github`

**Flow:**
1. User clicks "Login with Google/GitHub"
2. Redirected to OAuth2 provider
3. User authorizes the application
4. Redirected back to app with authorization code
5. Backend exchanges code for user info
6. Backend creates/finds user and generates JWT
7. Redirected to frontend with JWT token: `http://localhost:3000/oauth2/redirect?token={JWT}`

---

## Authorization Model

### Roles
- **ADMIN**: Full access (CRUD operations on books, comments, ratings)
- **USER**: Can rate and comment on books

### Permissions Matrix

| Action | Public | USER | ADMIN |
|--------|--------|------|-------|
| Browse books (GET /books) | ✅ | ✅ | ✅ |
| View book details (GET /bookid/{id}) | ✅ | ✅ | ✅ |
| Search books (GET /books/search) | ✅ | ✅ | ✅ |
| View ratings (GET /book/{id}/ratings) | ✅ | ✅ | ✅ |
| View comments (GET /book/{id}/comment) | ✅ | ✅ | ✅ |
| Add book (POST /addbook) | ❌ | ❌ | ✅ |
| Update book (PUT /book/{id}) | ❌ | ❌ | ✅ |
| Delete book (DELETE /book/{id}) | ❌ | ❌ | ✅ |
| Add rating (POST /book/{id}/rating) | ❌ | ✅ | ✅ |
| Add comment (POST /book/{id}/comment) | ❌ | ✅ | ✅ |
| Update own comment (PUT /book/{id}/comment) | ❌ | ✅ (own) | ✅ |
| Delete own comment (DELETE /comment/{id}) | ❌ | ✅ (own) | ✅ (any) |

---

## Public vs Protected Endpoints

### Public Endpoints (No Authentication Required)
```
GET  /books                    - List all books (paginated)
GET  /bookid/{id}              - Get book details
GET  /books/search             - Search books by title/author/category
GET  /book/{id}/ratings        - Get rating statistics
GET  /book/{id}/comment        - Get comments for a book
POST /register                 - Register new user
POST /login                    - Login with credentials
GET  /oauth2/authorization/*   - OAuth2 login endpoints
```

### Protected Endpoints (Authentication Required)
```
POST   /addbook                - Add new book (ADMIN only)
PUT    /book/{id}              - Update book (ADMIN only)
DELETE /book/{id}              - Delete book (ADMIN only)
POST   /book/{id}/rating       - Add rating (USER/ADMIN)
POST   /book/{id}/comment      - Add comment (USER/ADMIN)
PUT    /book/{id}/comment      - Update comment (USER/ADMIN, ownership check)
DELETE /comment/{commentId}    - Delete comment (USER/ADMIN, ownership check)
```

---

## OAuth2 Integration

### Configuration

**Google OAuth2:**
- Client ID: Configured in `application.properties`
- Scopes: `profile`, `email`
- Redirect URI: `http://localhost:8080/login/oauth2/code/google`

**GitHub OAuth2:**
- Client ID: Configured in `application.properties`
- Scopes: `user:email`
- Redirect URI: `http://localhost:8080/login/oauth2/code/github`

### OAuth2 Flow Implementation

The `OAuth2LoginSuccessHandler` handles successful OAuth2 authentication:
1. Extracts user email and name from OAuth2 provider
2. Creates new user if doesn't exist (role: USER)
3. Generates JWT token with user info
4. Redirects to frontend with token parameter

### Frontend Integration Example
```javascript
// Redirect user to OAuth2 provider
window.location.href = 'http://localhost:8080/oauth2/authorization/google';

// Handle callback
const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');
if (token) {
  localStorage.setItem('jwt_token', token);
  // Redirect to dashboard
}
```

---

## JWT Token Management

### Token Structure
```json
{
  "sub": "user@example.com",
  "role": "USER",
  "roleVersion": 0,
  "userId": 123,
  "iat": 1634567890,
  "exp": 1634569690
}
```

### Token Types
1. **Access Token**: Short-lived (30 minutes), used for API requests
2. **Refresh Token**: Long-lived (7 days), used to get new access tokens

### Using JWT in Requests
```bash
# Include JWT in Authorization header
curl -H "Authorization: Bearer {JWT_TOKEN}" \
     http://localhost:8080/book/1/comment
```

### Token Expiration Handling
- Access tokens expire after 30 minutes
- Frontend should handle 401 responses and refresh tokens
- Role version changes invalidate all existing tokens

### Security Headers
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## Security Best Practices

### Implemented

1. **Password Security**
   - BCrypt hashing with strength 12
   - No plain text storage

2. **Token Security**
   - Short expiration times
   - Signature validation
   - Role version tracking for invalidation

3. **Input Validation**
   - Comment length limited to 1000 characters
   - Rating values validated (1-5)
   - Empty content rejected

4. **Ownership Validation**
   - Users can only modify their own comments
   - Admins can delete any comment

5. **Error Handling**
   - Generic error messages (no info leakage)
   - Proper HTTP status codes
   - Detailed logging (server-side only)

### Recommended Additional Measures

1. **Production Security**
   ```properties
   # Generate strong JWT secret (DO NOT use in production as-is)
   jwt.secret=USE_OPENSSL_RAND_BASE64_32_TO_GENERATE
   
   # Use environment variables
   JWT_SECRET=${JWT_SECRET}
   GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
   GITHUB_CLIENT_SECRET=${GITHUB_CLIENT_SECRET}
   ```

2. **Rate Limiting**
   - Add rate limiting for login/register endpoints
   - Prevent brute force attacks

3. **HTTPS**
   - Use HTTPS in production
   - Enable HSTS headers

4. **CORS Configuration**
   - Restrict allowed origins in production
   - Update `WebConfig.java` with specific domains

5. **Database Security**
   - Use database user with minimum required privileges
   - Enable SSL for database connections

---

## Testing OAuth2 Flow

### Google OAuth2 Testing
1. Navigate to: `http://localhost:8080/oauth2/authorization/google`
2. Login with Google account
3. Grant permissions
4. Verify redirect with JWT token
5. Use token for authenticated requests

### GitHub OAuth2 Testing
1. Navigate to: `http://localhost:8080/oauth2/authorization/github`
2. Login with GitHub account
3. Grant permissions
4. Verify redirect with JWT token
5. Use token for authenticated requests

### Testing Authenticated Endpoints
```bash
# Login
TOKEN=$(curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user@example.com","password":"password123"}' \
  | jq -r '.token')

# Use token
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/book/1/comment
```

---

## Configuration Files

### application.properties
Key security configurations:
- JWT secret and expiration times
- OAuth2 client credentials
- Session management
- Error message inclusion

### SecurityConfig.java
- Defines public vs protected endpoints
- Configures OAuth2 login
- Sets up JWT filter chain
- Configures password encoder

### JwtFilter.java
- Validates JWT tokens
- Sets authentication context
- Handles token errors gracefully

---

## Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - Token expired (check expiration time)
   - Invalid token signature
   - Role version mismatch

2. **403 Forbidden**
   - User doesn't have required role
   - Attempting to modify another user's content

3. **OAuth2 Redirect Issues**
   - Check redirect URI configuration
   - Verify OAuth2 app credentials
   - Ensure frontend URL is correct

### Debug Logging
Enable debug logging in `application.properties`:
```properties
logging.level.com.marvel.springsecurity=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

## Summary

This implementation provides:
- ✅ Secure JWT-based authentication
- ✅ OAuth2 integration for social login
- ✅ Role-based access control
- ✅ Public browsing without authentication
- ✅ Ownership validation for user-generated content
- ✅ Industry-standard security practices

All critical endpoints are protected, and the system follows the principle of least privilege.


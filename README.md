# Book Shelf Backend API

Spring Boot RESTful API for book management with JWT authentication, OAuth2, and role-based access control.

## Features

- 📚 Book management (CRUD, search, filtering)
- 🔐 JWT & OAuth2 authentication (Google, GitHub)
- 👥 Role-based access control (Admin, User)
- ⭐ Ratings and comments
- 🛡️ Password encryption & token expiration

## Tech Stack

- Spring Boot 3.x
- Java 17+
- Maven
- JWT + OAuth2
- Spring Security 6.x

## Prerequisites

- Java 17+
- Maven 3.8+
- Git
- Database (MySQL/PostgreSQL)

## Quick Start

```bash
# Clone repository
git clone https://github.com/mohan6623/springSecurity.git
cd springSecurity

# Configure environment (application.properties or .env)
# - Database URL, username, password
# - JWT secret and expiration
# - OAuth2 credentials (Google, GitHub)

# Build and run
mvn clean install
mvn spring-boot:run
```

Server runs on `http://localhost:8080`

## Configuration

Set these properties in `application.properties` or environment variables:

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/book_shelf
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=password

# JWT
JWT_SECRET=your-secret-key-min-32-chars
JWT_EXPIRATION=1800000

# OAuth2 (Google & GitHub)
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=...
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=...
```

## Project Structure

```
src/main/java/com/marvel/springsecurity/
├── config/          # Security configuration
├── controller/      # REST endpoints
├── dto/             # Data transfer objects
├── model/           # Entity models
├── repo/            # Repositories
└── service/         # Business logic
```

## API Endpoints

### Authentication
- `POST /register` - Register new user
- `POST /login` - Login with credentials
- `GET /oauth2/authorization/google` - Google OAuth2
- `GET /oauth2/authorization/github` - GitHub OAuth2

### Books (Public)
- `GET /books?page=0&size=10` - List books
- `GET /bookid/{id}` - Get book details
- `GET /books/search?query=...` - Search books

### Books (Admin Only)
- `POST /addbook` - Add new book
- `PUT /book/{id}` - Update book
- `DELETE /book/{id}` - Delete book

### Ratings (Public)
- `GET /book/{id}/ratings` - Get ratings

### Ratings (Authenticated)
- `POST /book/{id}/rating` - Add rating

### Comments (Public)
- `GET /book/{id}/comment` - Get comments

### Comments (Authenticated)
- `POST /book/{id}/comment` - Add comment
- `PUT /book/{id}/comment` - Edit comment
- `DELETE /comment/{id}` - Delete comment

## Authorization

| Role | Permissions |
|------|-------------|
| **Public** | Browse, view books & comments |
| **USER** | Add ratings, comments; edit own comments |
| **ADMIN** | All USER + add/edit/delete books, delete any comment |

## Security

- JWT tokens expire in 30 minutes
- Password encryption with BCrypt (strength: 12)
- Role version tracking for token invalidation
- Ownership validation for user content
- Input validation & sanitization
- CORS support

## Building & Testing

```bash
# Run tests
mvn test

# Build JAR
mvn clean package

# Run JAR
java -jar target/Book-Forum-API.jar
```

## Client Integration

```javascript
// Store JWT after login
localStorage.setItem('jwt_token', response.token);

// Add to request headers
headers: {
  'Authorization': `Bearer ${localStorage.getItem('jwt_token')}`
}

// Handle OAuth2 callback
const token = new URLSearchParams(window.location.search).get('token');
if (token) localStorage.setItem('jwt_token', token);
```

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| **401 Unauthorized** | Expired/invalid token | Refresh token or re-login |
| **403 Forbidden** | Insufficient permissions | Check user role & ownership |
| **Database error** | Connection failed | Verify DB is running & config |
| **OAuth2 error** | Redirect URI mismatch | Check provider configuration |

## Contributing

1. Fork the repo
2. Create feature branch (`git checkout -b feature/xyz`)
3. Commit changes (`git commit -m 'Add xyz'`)
4. Push branch (`git push origin feature/xyz`)
5. Open Pull Request

## License

MIT License - See LICENSE file for details

---

**Version**: 1.0.0 | **Last Updated**: December 2024

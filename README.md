# 📚 Book Forum Backend

> Backend API for a book community platform.  
> Users browse books, rate & review; admins manage the catalog.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue)](https://www.postgresql.org/)

---

## 🎯 About

**Book Forum** is a backend API for a book community platform — think of it as the engine behind a reading app where people discover, discuss, and share their love for books.

### How It Works
1. **Readers** browse the book catalog, search by title/author/category
2. **Users** sign up (email or Google/GitHub), verify email, rate books, leave comments
3. **Admins** add new books, upload covers, manage the catalog
4. **Security** handles authentication, authorization, and rate limiting automatically



## ✨ Features

| Category | Features |
|----------|----------|
| **Auth** | JWT tokens, OAuth2 (Google/GitHub), email verification, password reset |
| **Books** | CRUD, search, filter, pagination, categories, authors |
| **Social** | Ratings, comments, user profiles |
| **Security** | BCrypt, rate limiting, CORS, input validation |
| **Cloud** | Cloudinary images, Swagger docs, health checks |

---

## 🛠️ Tech Stack

![Spring](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![JSON Web Tokens](https://img.shields.io/badge/JWT-FB015B?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![Google](https://img.shields.io/badge/OAuth2-4285F4?style=for-the-badge&logo=google&logoColor=white)
![GitHub](https://img.shields.io/badge/OAuth2-181717?style=for-the-badge&logo=github&logoColor=white)
![Cloudinary](https://img.shields.io/badge/Cloudinary-3448C5?style=for-the-badge&logo=cloudinary&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Apache Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)


---

## 🚀 Quick Start

```bash
# Clone
git clone https://github.com/yourusername/book-forum-backend.git
cd book-forum-backend

# Configure (see .env.example)
cp .env.example .env
# Edit .env with your credentials

# Run
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

---

## ⚙️ Configuration

Create `.env` file with:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/book_forum
SPRING_DATASOURCE_USERNAME=your_user
SPRING_DATASOURCE_PASSWORD=your_password

# JWT
JWT_SECRET=your_secret_key_min_32_chars
JWT_EXPIRATION=1800000

# OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_secret
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_secret

# Email (SMTP)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_USERNAME=your_email
SPRING_MAIL_PASSWORD=your_app_password

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud
CLOUDINARY_API_KEY=your_key
CLOUDINARY_API_SECRET=your_secret

# Frontend URL
APP_FRONTEND_URL=http://localhost:3000
```

---

## 📡 API Endpoints

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/register` | Register user |
| POST | `/api/login` | Login |
| GET | `/oauth2/authorization/google` | Google OAuth |
| GET | `/oauth2/authorization/github` | GitHub OAuth |
| POST | `/api/forgot-password` | Password reset |

### Books
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/books` | List books (paginated) |
| GET | `/api/bookid/{id}` | Get book |
| GET | `/api/books/search` | Search books |
| POST | `/api/addbook` | Add book (Admin) |
| PUT | `/api/book/{id}` | Update book (Admin) |
| DELETE | `/api/book/{id}` | Delete book (Admin) |

### Ratings & Comments
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/book/{id}/ratings` | Get ratings |
| POST | `/api/book/{id}/rating` | Add rating |
| GET | `/api/book/{id}/comment` | Get comments |
| POST | `/api/book/{id}/comment` | Add comment |

### User
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/user/profile` | Get profile |
| PATCH | `/api/user/update-name` | Update name |
| GET | `/api/health` | Health check |

**Interactive API Documentation:** [http://api.backend.app/swagger-ui/index.html](http://api.backend.app/swagger-ui/index.html)

---

## 🔒 Authentication

### JWT Flow
1. Login → receive `token` + `refreshToken`
2. Include `Authorization: Bearer <token>` in requests
3. Token expires in 30 min, refresh token in 7 days

### OAuth2 Flow
1. Redirect to `/oauth2/authorization/google` or `/github`
2. User authenticates with provider
3. Callback redirects to frontend with JWT

---

## 🗄️ Database

```
Users ──┬── OAuthProvider (1:N)
        ├── Comment (1:N)
        └── Rating (1:N)

Book ───┬── Comment (1:N)
        └── Rating (1:N)
```

**Tables:** `users`, `book`, `comment`, `rating`, `oauth_provider`

---

## 🧪 Testing

```bash
mvn test                          # All tests
mvn test -Dtest=BookControllerTest  # Specific test
```

---

## 📦 Build & Deploy

```bash
# Build JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/Book-Forum-API.jar
```

---

## 📄 License

MIT License - See [LICENSE](LICENSE)

---

**Author:** [Mohan](https://github.com/mohan6623)

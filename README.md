# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- Interactive seat selection and Razorpay integration
- **Coupon & Promotional Discount Engine:** Percentage and flat-rate promo codes with real-time UI validation, tamper-proof server-side recalculation, minimum spend thresholds, usage limits, and expiration controls
- Email ticket confirmation and PDF download with QR code verification
- Responsive UI with Bootstrap
- Secure login and password reset via OTP

---

## 🛠️ Technologies Used

- **Backend:** Spring Boot 3, Java 17, Spring Data JPA, Spring Security
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5, jQuery
- **Database:** PostgreSQL / MySQL
- **Email & PDF:** JavaMailSender, OpenPDF / iText, ZXing for QR Code
- **Payment:** Razorpay integration
- **Security:** Role-based access control (RBAC), BCrypt password hashing

---

## 🏷️ Coupon & Promotional Discount Engine

MovieMatic includes a promotional discount engine:
- **Discount Models:** Percentage (`PERCENTAGE`) and flat amount (`FLAT`) discounts.
- **Tamper Resistance:** Server-side price recalculation upon booking persistence prevents client-side price manipulation.
- **Usage & Expiration:** Expiration timestamps (`valid_until`) and global redemption caps (`usage_limit`).
- **Spend Thresholds:** Configurable minimum booking subtotal amounts (`min_order_amount`).
- **REST Validation API:**
  - `POST /user/coupons/validate` with `{ "code": "SAVE20", "totalAmount": 500.0 }` returns discount breakdown.
  - `POST /user/bookings` accepts `{ "showId": 1, "seatNumbers": "A1,A2", "couponCode": "SAVE20" }` and persists the discounted booking.

---

## 👤 Admin Role: Features and Flow

The **Admin** is responsible for managing all data and monitoring platform usage.

### Dashboard
- Displays total bookings, revenue, movies, and shows
- Shows recent activity: latest movies/shows/bookings

### 🎞️ Movie Management
- Add/Edit/Delete/Search movies
- Upload poster and background images (stored as BLOBs)

### 📅 Show Management
- Add/Edit/Delete shows
- Set show time, screen, date, price, and linked movie

### 🎟️ Booking Management
- View all bookings, search/filter by user/movie/date
- View applied coupon codes and discount amounts
- View user info and delete bookings if needed

### 🔒 Security
- Role-based access and secure session management
- Sidebar navigation for quick access to all sections

---

## 🙋‍♂️ User Role: Features and Flow

The **User** experiences a smooth, engaging interface to explore and book movie tickets.

### 🏠 Dashboard
- Scrollable carousels for featured, top, and upcoming movies
- Visual UI encouraging movie discovery

### 🔍 Movie Discovery
- Browse All Movies page
- Filter by genre or search by name
- View detailed movie pages with cast, synopsis, shows

### 🎫 Booking Flow
- Interactive seat layout (`/user/book?showId=...`): select seats, view subtotal
- Checkout modal with promo code application and real-time discount line-item
- Razorpay integration with discounted payable amount
- Booking confirmation with discounted total and coupon tracking

### 📧 Email + PDF Ticket
- Booking confirmation sent via email
- PDF ticket available for download anytime with QR code

### 👤 Profile Management
- Update personal details and password
- “Forgot Password” with OTP email verification

---

## 🔐 Security Highlights

- Passwords encrypted using BCrypt
- Role-based authorization for `/admin/**` and `/user/**`
- Server-side price recalculation preventing coupon abuse and price tampering
- CSRF configured for payment and AJAX workflows

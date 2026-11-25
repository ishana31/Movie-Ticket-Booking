# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- Interactive seat selection and Razorpay integration
- **Coupon & Promotional Discount Engine** (Percentage & Flat discounts with server-side validation)
- Email ticket confirmation and PDF download
- Responsive UI with Bootstrap
- Secure login and password reset via OTP

---

## 🎟️ Coupon & Promotional Discount Engine

MovieMatic features a server-validated promotional discount engine:
- **Coupon Models**: Supports `PERCENTAGE` (e.g., `SAVE20` for 20% off) and `FLAT` rate discounts (e.g., `FLAT50`).
- **Server-Side Price Authority**: Discounts are recalculated server-side during booking creation to prevent client-side tampering.
- **Constraints & Thresholds**:
  - Expiration dates (`valid_until`)
  - Platform-wide redemption caps (`usage_limit` and `used_count`)
  - Minimum gross booking threshold (`min_order_amount`)
  - Case-insensitive promo code lookup (`save20` == `SAVE20`)
- **Interactive UI**:
  - Checkout modal coupon input with instant "Apply" feedback
  - Real-time line-item discount display and updated payable total
  - Informative alerts for expired, invalid, or unmet threshold conditions
- **REST Endpoints**:
  - `POST /user/coupons/validate`: Validates code and previews discounts.
  - `POST /user/bookings`: Submits booking with recalculation and usage tracking.

---

## 🛠️ Technologies Used

- **Backend:** Spring Boot, Java, Spring Security, Spring Data JPA
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5, jQuery
- **Database:** PostgreSQL / MySQL
- **Email & PDF:** JavaMailSender, iText/OpenPDF, ZXing for QR Code
- **Payment:** Razorpay integration
- **Security:** Role-based login, OTP password reset

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

### 🎫 Booking Flow & Discounts
- Interactive seat layout: select, view price, and pay
- Real-time promo code validation and discount application
- Razorpay integration for secure transactions
- Booking summary with selected seats, discount details, and total amount

### 📧 Email + PDF Ticket
- Booking confirmation sent via email
- PDF ticket available for download anytime

### 👤 Profile Management
- Update personal details and password
- “Forgot Password” with OTP email verification

### 📱 Fully Responsive UI
- Works smoothly across mobile, tablet, and desktop devices

---

## 📋 Completed Backlog Scope

- [x] **KAN-12**: Coupon & Promotional Discount Engine
  - [x] Database table `coupons` with type, value, expiry, threshold, limit, and active state
  - [x] Booking entity updated with `coupon_code` and `discount_amount`
  - [x] `CouponRepository` with `findByCodeIgnoreCaseAndIsActiveTrue`
  - [x] `CouponService` with percentage/flat calculations, minimum spend, expiry, and usage limits
  - [x] Checkout modal UI with promo code entry, "Apply" button, real-time feedback, and alerts
  - [x] `POST /user/coupons/validate` endpoint
  - [x] `POST /user/bookings` endpoint returning `{ bookingId, totalAmount, discountAmount }`
  - [x] Automated unit and integration tests

---

## 🔐 Security Highlights

- JWT or session-based login
- Passwords encrypted using BCrypt
- OTP-based password recovery
- Admin features protected by role-based access control
- Discounts strictly verified server-side before persisting bookings

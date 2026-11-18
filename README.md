# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- Interactive seat selection and Razorpay integration
- **Promotional Discounts & Coupon Engine (KAN-12)**:
  - Percentage and flat-rate promo codes (e.g. `SAVE20`, `FLAT50`)
  - Real-time coupon validation, min spend checks, and instant UI total recalculation
  - Server-side price recalculation preventing client-side tampering
  - Expiry and maximum usage limits enforcement
- Email ticket confirmation and PDF download
- Responsive UI with Bootstrap
- Secure login and password reset via OTP

---

## 🛠️ Technologies Used

- **Backend:** Spring Boot, Java, Spring Security
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5, jQuery
- **Database:** PostgreSQL / MySQL, Spring Data JPA
- **Email & PDF:** JavaMailSender, OpenPDF / iText, ZXing for QR Code
- **Payment:** Razorpay integration
- **Security:** Role-based access control, BCrypt password hashing

---

## 🎟️ Coupon & Promotional Discount Engine

### Endpoints
- `POST /user/coupons/validate`
  - Validates promo code validity, expiration date, usage limits, and minimum order threshold.
  - Body: `{ "code": "SAVE20", "totalAmount": 500.0 }`
  - Response: `200 OK` with `{ "valid": true, "discountAmount": 100.0, "finalAmount": 400.0 }`
  - Error: `400 Bad Request` with `{ "message": "Invalid or expired coupon code" }` or minimum spend warning.

- `POST /user/bookings` (Updated)
  - Recalculates seat prices and discounts server-side before booking creation.
  - Body: `{ "showId": 1, "seatNumbers": "A1,A2", "couponCode": "SAVE20" }`
  - Response: `200 OK` with `{ "bookingId": 1, "totalAmount": 400.0, "discountAmount": 100.0 }`

---

## 📋 Feature Backlog
- [x] KAN-12: Coupon & Promotional Discount Engine

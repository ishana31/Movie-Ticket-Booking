# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- Interactive seat selection and Razorpay integration
- **Coupon & Promotional Discount Engine:** Percentage and flat-rate discounts with minimum spend and usage threshold validation
- Email ticket confirmation and PDF download
- Responsive UI with Bootstrap
- Secure login and password reset via OTP

---

## 🛠️ Technologies Used

- **Backend:** Spring Boot, Java, Spring Security
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5
- **Database:** MySQL/PostgreSQL
- **Email & PDF:** JavaMailSender, iText/Apache PDFBox, ZXing for QR Code
- **Payment:** Razorpay integration
- **Security:** Role-based login, OTP password reset

---

## 🏷️ Coupon & Promotional Discount Engine (KAN-13)

MovieMatic features a promotional discount engine that applies coupons at checkout:
- **Case-Insensitive Codes:** Matches promo codes regardless of case (e.g. `save20` == `SAVE20`).
- **Flexible Discount Types:** Supports both `PERCENTAGE` and `FLAT` rate discounts.
- **Strict Server-Side Validation:** Recalculates discounts on the backend during booking creation to protect against client-side price tampering.
- **Eligibility Criteria:** Validates expiration dates, maximum redemption limits (`usage_limit`), and minimum order amount (`min_order_amount`).
- **Checkout Modal UI:** Real-time coupon application with instant savings summary and helpful error messages.

---

## 📋 Feature Backlog & Roadmap

- [x] **KAN-13: Coupon & Promotional Discount Engine** (Percentage/Flat discounts, validation endpoint, server recalculation, checkout modal integration)
- [x] Interactive seat selection & checkout
- [x] Razorpay payment integration
- [x] Automated confirmation email and PDF ticket generation

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

### 🎫 Booking Flow
- Interactive seat layout: select, view price, and pay
- Apply promotional coupons in the checkout modal (`SAVE20`, `FLAT50`)
- Razorpay integration for secure transactions
- Booking summary with selected seats, applied discounts, and payable total

### 📧 Email + PDF Ticket
- Booking confirmation sent via email
- PDF ticket available for download anytime

### 👤 Profile Management
- Update personal details and password
- “Forgot Password” with OTP email verification

### 📱 Fully Responsive UI
- Works smoothly across mobile, tablet, and desktop devices

---

## 🧾 Summary Table – Admin vs. User

| Feature/Section     | Admin Capabilities                                    | User Capabilities                                      |
|---------------------|--------------------------------------------------------|--------------------------------------------------------|
| **Dashboard**       | View stats, recent movies/shows/bookings              | View carousels, top movies, upcoming releases         |
| **Movie Management**| Add, edit, delete, search movies                      | Browse, search, filter movies                         |
| **Show Management** | Add, edit, delete shows                               | View shows, book tickets                              |
| **Booking Management**| View all bookings, delete, view user info          | View, delete own bookings, download PDF               |
| **Coupon Engine**   | Inspect applied coupons on bookings                    | Validate & apply promo codes at checkout modal        |
| **Profile Management**| (Optional)                                          | Update profile, change password, OTP reset            |
| **Payments**        | View revenue stats                                    | Razorpay checkout for booking                         |
| **Email Integration**| Optional for alerts                                  | Confirmation emails for bookings & reset              |
| **Security**        | Role-based control, session handling                  | Secure login, OTP reset                               |

---

## 🔐 Security Highlights

- JWT or session-based login
- Passwords encrypted using BCrypt
- OTP-based password recovery
- Admin features protected by role-based access control
- Server-side price & discount recalculation preventing checkout tampering

---

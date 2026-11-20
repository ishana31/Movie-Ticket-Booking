# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- Interactive seat selection and Razorpay integration
- **Promotional Coupon Engine (KAN-13)**: Percentage and flat discounts, spend thresholds, usage limits, case-insensitive coupon validation, and server-side discount recalculation
- Email ticket confirmation and PDF download with QR Code
- Responsive UI with Bootstrap 5
- Secure login and password reset via OTP

---

## 🛠️ Technologies Used

- **Backend:** Spring Boot, Java, Spring Security, Spring Data JPA
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5, jQuery
- **Database:** PostgreSQL / MySQL
- **Email & PDF:** JavaMailSender, iText / LibrePDF OpenPDF, ZXing for QR Code
- **Payment:** Razorpay integration
- **Security:** Role-based access control, BCrypt encryption, OTP password reset

---

## 🎟️ Coupon & Promotional Discount Engine (KAN-13)

Users can apply promotional coupon codes during checkout to receive percentage or flat-rate savings.

### Key Capabilities:
- **Case-Insensitive Promo Codes:** Codes like `SAVE20` and `save20` are handled seamlessly.
- **Dynamic Real-Time UI Preview:** Validates code against active dates, usage limits, and minimum spend requirements directly from the booking modal.
- **Server-Side Price Tamper Prevention:** The booking creation endpoint (`POST /user/bookings`) recalculates all discounts on the server before persisting the booking and generating payment orders.
- **Usage Tracking:** Atomically tracks coupon redemption counts against configured limits.

### API Endpoints:
- `POST /user/coupons/validate`: Validates coupon code and returns discount and payable totals.
- `POST /user/bookings`: Confirms reservation with server-validated coupon discounts.

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

### 🎫 Booking & Coupon Flow
- Interactive seat layout: select seats, view running total
- Coupon redemption with instant discount breakdown
- Razorpay integration for secure transactions on discounted total
- Booking summary with selected seats, discount line item, and final total

### 📧 Email + PDF Ticket
- Booking confirmation sent via email with PDF ticket attached
- PDF ticket available for download anytime with QR Code

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
| **Promotions**      | Manage promotional campaigns                          | Apply promo codes, view discounts                     |
| **Booking Management**| View all bookings, delete, view user info          | View, cancel own bookings, download PDF               |
| **Profile Management**| (Optional)                                          | Update profile, change password, OTP reset            |
| **Payments**        | View revenue stats                                    | Razorpay checkout with discounted totals              |
| **Email Integration**| Alerts and notifications                             | Confirmation emails with PDF attachment               |
| **Security**        | Role-based control, session handling                  | Secure login, OTP reset                               |

---

## 🔐 Security Highlights

- Role-based access control (`ROLE_ADMIN`, `ROLE_USER`)
- Passwords encrypted using BCrypt
- OTP-based password recovery
- Server-side price recalculation preventing client tampering

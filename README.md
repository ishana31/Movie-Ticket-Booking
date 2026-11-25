# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- Interactive seat selection and Razorpay integration
- **Promotional Discount & Coupon Engine** (Percentage and Flat discounts, minimum spend validation, expiration, and usage limits)
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

## 🎟️ Coupon & Promotional Discount Engine (KAN-13)

MovieMatic features a promotional discount engine supporting promotional codes during ticket booking.

### Key Capabilities
- **Percentage & Flat Discounts:** Supports both percentage off (e.g. 20% off) and fixed cash discount (e.g. ₹50 off).
- **Cart Thresholds:** Enforces minimum order booking amounts (e.g., minimum booking amount of ₹300.00 required).
- **Expiration & Usage Caps:** Time-bounded promotional windows (`valid_until`) and global redemption limits (`usage_limit`).
- **Server-Side Validation:** Discounts and payable balances are recalculated on the server during booking creation to prevent client-side tampering.
- **Interactive Checkout Modal:** Real-time coupon application and live price recalculation in the booking modal on `http://localhost:8080/user/book?showId={id}`.

### Endpoints
- `POST /user/coupons/validate` - Validates promo code against current cart total and returns discount and final payable amounts.
- `POST /user/bookings` - Finalizes booking with applied coupon code and discounted total.

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
- Interactive seat layout: select, view price, and apply promo codes
- Real-time coupon validation with immediate discount feedback
- Razorpay integration for secure transactions with final discounted amounts
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

## 🧾 Summary Table – Admin vs. User

| Feature/Section     | Admin Capabilities                                    | User Capabilities                                      |
|---------------------|--------------------------------------------------------|--------------------------------------------------------|
| **Dashboard**       | View stats, recent movies/shows/bookings              | View carousels, top movies, upcoming releases         |
| **Movie Management**| Add, edit, delete, search movies                      | Browse, search, filter movies                         |
| **Show Management** | Add, edit, delete shows                               | View shows, book tickets                              |
| **Booking Management**| View all bookings, delete, view user info          | View, delete own bookings, download PDF               |
| **Coupons & Promos**| Manage promotional codes                              | Apply promo codes, receive instant discounts           |
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

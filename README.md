# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- Interactive seat selection and Razorpay integration
- Promotional Discount Engine with percentage and flat coupons (KAN-13)
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
- Interactive seat layout: select seats and view calculated price
- Checkout modal with promo code support (percentage and flat discounts)
- Real-time server-side validation preventing client price tampering
- Razorpay integration for secure transactions at the discounted total
- Booking summary with selected seats, discount line item, and final payable amount

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
| **Coupons & Promos**| Configure promotional discount codes                   | Validate and apply promo codes at checkout             |
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
- Server-side price recalculation and coupon verification to prevent tampering

---

## 📋 Completed Backlog
- [x] **KAN-13**: Coupon & Promotional Discount Engine (validation, recalculation, modal UI, and living docs)

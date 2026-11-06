# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- Interactive seat selection and Razorpay integration
- Email ticket confirmation and PDF download
- **Self-Service Ticket Cancellation with Tiered Refund Policy & Automatic Seat Release (KAN-10)**
- Responsive UI with Bootstrap
- Secure login and password reset via OTP

---

## 🛠️ Technologies Used

- **Backend:** Spring Boot, Java, Spring Security
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5
- **Database:** MySQL/PostgreSQL/H2 (test)
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
- View user info, status badges (CONFIRMED/CANCELLED), and manage bookings
- Audit cancellation timestamps and refund amounts

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
- Razorpay integration for secure transactions
- Booking summary with selected seats and total amount

### ❌ Self-Service Ticket Cancellation & Refund (KAN-10)
- **Self-Service Cancellation:** Customers can cancel their tickets directly from the "My Bookings" page.
- **Confirmation & Preview Modal:** Real-time preview breakdown showing Total Paid, Refund Percentage, Cancellation Fee, and Refund Amount before confirming.
- **Tiered Refund Policy:**
  - **> 4 hours before show start:** 90% refund (10% cancellation fee).
  - **1 to 4 hours before show start:** 50% refund (50% cancellation fee).
  - **< 1 hour before show start:** Ineligible for cancellation or refund.
  - **Show has started / passed:** Ineligible for cancellation.
- **Instant Seat Release:** Cancelled seats are immediately freed and made available for other users to book in real-time.
- **Audit & Record Keeping:** Booking records are preserved with status `CANCELLED`, along with `cancelled_at` timestamp, `refund_amount`, `cancellation_fee`, and `refund_status`.

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
| **Booking Management**| View all bookings, view status badges, user info     | View own bookings, preview refund, cancel ticket, download PDF |
| **Profile Management**| (Optional)                                          | Update profile, change password, OTP reset            |
| **Payments**        | View revenue stats                                    | Razorpay checkout for booking                         |
| **Email Integration**| Optional for alerts                                  | Confirmation emails for bookings & reset              |
| **Security**        | Role-based control, session handling                  | Secure login, OTP reset, booking ownership check      |

---

## 🔐 Security Highlights

- JWT or session-based login
- Passwords encrypted using BCrypt
- OTP-based password recovery
- Admin features protected by role-based access control
- Booking cancellation strictly enforced by authenticated user ownership (`403 Forbidden` on unauthorized attempts)

---

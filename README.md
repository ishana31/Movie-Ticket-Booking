# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- **Concurrency-Safe Seat Reservation System with Temporary Hold & Auto-Release (TTL)**:
  - 10-minute temporary seat hold with live countdown timer
  - 3 distinct seat states: Available (Green), Held (Orange), Booked (Red)
  - Concurrency safety with atomic reservation and single-owner verification
  - Automatic background release scheduler every 60 seconds
- Interactive seat selection and Razorpay integration
- Email ticket confirmation and PDF download
- Responsive UI with Bootstrap
- Secure login and password reset via OTP

---

## 🛠️ Technologies Used

- **Backend:** Spring Boot, Java, Spring Security, Spring Task Scheduling
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5, jQuery
- **Database:** PostgreSQL / MySQL
- **Email & PDF:** JavaMailSender, OpenPDF / iText, ZXing for QR Code
- **Payment:** Razorpay integration
- **Security:** Role-based login, BCrypt password hashing, OTP reset

---

## 🎟️ Seat Reservation & Temporary Hold (KAN-11)

### Features & Workflow
- **Seat Selection**: Customers select available seats and click "Confirm Booking". Selected seats transition immediately to `HELD` status.
- **10-Minute Countdown Timer**: Checkout modal displays real-time countdown `10:00 -> 00:00`.
- **Double-Booking Prevention**: Other customers see held seats rendered in Orange (disabled). If a conflict occurs, an alert warns: *"Selected seat(s) are currently reserved by another customer. Please choose different seats."*
- **Auto-Release (TTL)**: Holds strictly expire after 10 minutes. A background scheduled task running every 60 seconds automatically releases expired holds back to `AVAILABLE` (Green).
- **Payment Confirmation**: Upon payment success, seats transition permanently to `BOOKED` (Red).

### Endpoints
- `POST /user/shows/{showId}/hold`: Request body `{ "seatNumbers": "A1,A2" }`. Returns `200 OK` with `{ "status": "HELD", "expiresInSeconds": 600 }` or `409 Conflict`.
- `GET /user/shows/{showId}/occupied-seats`: Returns `{ "bookedSeats": [...], "heldSeats": [...] }`.
- `POST /user/bookings`: Confirms reservation with single-owner verification.

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
- Interactive seat layout with 3 status colors (Green, Orange, Red)
- 10-minute temporary hold with live countdown timer
- Razorpay integration for secure transactions
- Booking summary with selected seats and total amount

### 📧 Email + PDF Ticket
- Booking confirmation sent via email
- PDF ticket available for download anytime

---

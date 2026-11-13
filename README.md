# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- **Concurrency-Safe Seat Reservation System (KAN-11):**
  - Real-time temporary seat hold with 10-minute countdown timer (TTL)
  - Color-coded seat visualizer: Green (Available), Orange (Held), Red (Booked)
  - Auto-release background cleanup task running every 60 seconds
  - Single-owner purchase invariant & atomic multi-seat hold
  - Race-condition safe reservation under concurrent traffic
- Interactive seat selection and Razorpay integration
- Email ticket confirmation and PDF download
- Responsive UI with Bootstrap
- Secure login and password reset via OTP

---

## 🛠️ Technologies Used

- **Backend:** Spring Boot 3.5.0, Java 17, Spring Security, Spring Data JPA
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5, jQuery
- **Database:** PostgreSQL / MySQL
- **Email & PDF:** JavaMailSender, OpenPDF / iText, ZXing for QR Code
- **Payment:** Razorpay integration
- **Security:** Role-based access control, BCrypt encryption, OTP password reset

---

## 🎫 Real-Time Concurrency-Safe Seat Hold (KAN-11)

### Endpoints
- `POST /user/shows/{showId}/hold`:
  - Request: `{ "seatNumbers": "A1,A2" }`
  - Response: `200 OK` `{ "status": "HELD", "expiresInSeconds": 600 }`
  - Conflict: `409 Conflict` `{ "message": "Seats already held or booked" }`
- `GET /user/shows/{showId}/occupied-seats`:
  - Response: `200 OK` `{ "bookedSeats": ["B1", "B2"], "heldSeats": ["A1", "A2"] }`
- `POST /user/shows/{showId}/release-hold`:
  - Releases seats explicitly when user cancels or leaves checkout

### Business Rules & Invariants
- **Atomic Reservation:** All selected seats must be secured together; if one is unavailable, none are held.
- **Single Owner:** Only the user who placed the hold can complete the purchase.
- **Timeout (TTL):** Temporary holds strictly expire after 10 minutes.
- **Background Cleanup:** `@Scheduled` task running every 60 seconds auto-releasing expired seats back to `AVAILABLE`.

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
- Interactive seat layout with real-time status: Green (Available), Orange (Held), Red (Booked)
- 10-minute hold countdown timer modal during checkout
- Razorpay integration for secure transactions
- Booking summary with selected seats and total amount

### 📧 Email + PDF Ticket
- Booking confirmation sent via email
- PDF ticket available for download anytime

### 👤 Profile Management
- Update personal details and password
- “Forgot Password” with OTP email verification

---

## 🧾 Backlog & Release Progress

- [x] **KAN-11:** Implement Concurrency-Safe Seat Reservation System with Temporary Hold & Auto-Release (TTL)
  - [x] Database table `show_seats` with unique constraint `(show_id, seat_number)`
  - [x] Domain entity `ShowSeat` and `ShowSeatRepository`
  - [x] Service layer transactional locking in `SeatReservationService`
  - [x] Scheduled auto-release cleanup task running every 60s
  - [x] Endpoints `POST /user/shows/{showId}/hold` and `GET /user/shows/{showId}/occupied-seats`
  - [x] UI visualizer in `book.html` (3 distinct colors: Green, Orange, Red) and 10-min countdown modal
  - [x] Concurrency and atomic reservation automated tests

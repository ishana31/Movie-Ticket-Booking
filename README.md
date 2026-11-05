# 🎬 MovieMatic – Movie Ticket Booking Application

**MovieMatic** is a full-stack movie ticket booking web application built using **Spring Boot (Java)** for the backend and **Thymeleaf + Bootstrap** for the frontend. The platform supports two separate portals for **Admin** and **User**, each with customized dashboards, functionalities, and workflows.

---

## 🚀 Features at a Glance

- Role-based authentication: Admin & User
- Movie and Show management (CRUD) for Admin
- Dynamic Admin Dashboard with revenue, booking stats
- User dashboard with carousels and movie discovery
- Concurrency-safe seat reservation with 10-minute temporary holds & TTL auto-release
- Real-time seat status visualization: Available (Green), Held (Orange), Booked (Red)
- Interactive seat selection and Razorpay integration
- Email ticket confirmation and PDF download
- Responsive UI with Bootstrap
- Secure login and password reset via OTP

---

## 🎟️ Concurrency-Safe Seat Reservation System (KAN-9)

MovieMatic implements an enterprise-grade concurrency-safe seat reservation system preventing double-booking race conditions:

- **10-Minute Temporary Hold (TTL)**: When a customer clicks "Confirm Booking", selected seats are atomically transitioned to `HELD` status, locking them against other buyers while starting a 10-minute countdown timer.
- **Atomic Reservation**: All selected seats are secured as an atomic batch. If even one requested seat is already held or booked, none are reserved, and an alert is shown.
- **Single-Owner Checkout**: Only the customer who placed the temporary hold can complete checkout and pay for those seats.
- **Auto-Release Engine**: A background `@Scheduled` cleanup task automatically reverts abandoned holds back to `AVAILABLE` every 60 seconds once the 10-minute TTL expires.
- **Tri-Color Visual Seat Map**:
  - 🟢 **Green**: Available for selection
  - 🟠 **Orange**: Temporarily held by a customer with an active timer (disabled for others)
  - 🔴 **Red**: Permanently booked

### Endpoints
- `POST /user/shows/{showId}/hold`: Atomically locks seats for 600s (`{ "seatNumbers": "A1,A2" }`).
- `GET /user/shows/{showId}/occupied-seats`: Returns `{ "bookedSeats": [...], "heldSeats": [...] }`.
- `GET /user/book?showId={showId}`: Direct seat selection view by showId parameter.

---

## 🛠️ Technologies Used

- **Backend:** Spring Boot, Java, Spring Security
- **Frontend:** Thymeleaf, HTML5, CSS3, Bootstrap 5
- **Database:** PostgreSQL / MySQL, H2 (Testing)
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

### 🎫 Booking Flow
- Interactive seat layout: select, view price, and pay
- 10-minute hold lock during checkout
- Razorpay integration for secure transactions
- Booking summary with selected seats and total amount

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
| **Seat Holds**      | Monitor occupancy                                      | 10-min temporary hold with countdown timer             |
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

---

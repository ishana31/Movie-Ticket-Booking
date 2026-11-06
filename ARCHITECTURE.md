# MovieMatic System Architecture

## Overview
MovieMatic is a full-stack movie ticket booking web application built using **Spring Boot 3** and **Thymeleaf + Bootstrap 5**.

## Layered Architecture

1. **Controller Layer (`com.MovieMatic.controller`)**
   - `UserController`: Handles customer-facing views and REST endpoints (`/user/**`), including self-service booking, ticket downloads, profile management, and ticket cancellation.
   - `BookingController`: Admin endpoints (`/admin/bookings`) for viewing platform-wide booking activity.
   - `ShowController`, `MovieController`, `AuthController`, `EmailController`.

2. **Service Layer (`com.MovieMatic.service`)**
   - `BookingService`: Encapsulates business logic for bookings, seat occupancy tracking, tiered refund calculations, and cancellation processing.
   - `UserService`, `MovieService`, `ShowService`, `EmailService`, `orderServices`.

3. **Repository Layer (`com.MovieMatic.repository`)**
   - Spring Data JPA repositories interfacing with the relational schema (`BookingRepository`, `UserRepository`, `ShowRepository`, `MovieRepository`).

4. **Domain Model Layer (`com.MovieMatic.model`)**
   - Entities: `Booking`, `Show`, `Movie`, `User`.
   - Relationships annotated with `@JsonIgnore` to prevent infinite recursion during REST serialization.

5. **DTO Layer (`com.MovieMatic.dto`)**
   - `CancellationPreviewResponse`: Response model for the preview calculation endpoint containing `totalPaid`, `refundPercentage`, `refundAmount`, and `cancellationFee`.
   - `BookingRequest`: Request payload for booking operations.

## Self-Service Ticket Cancellation Design (KAN-10)

### Endpoints
- `GET /user/bookings/{bookingId}/cancellation-preview`
  - Validates authentication and ownership.
  - Verifies show start time > 1 hour.
  - Returns tiered breakdown (90% refund > 4h, 50% refund 1-4h).
- `POST /user/bookings/{bookingId}/cancel`
  - Atomically transitions booking status to `CANCELLED`.
  - Records cancellation timestamp and refund details.
  - Releasing seats in real-time by excluding cancelled bookings from occupied seats queries.

### Security
- Role-based authorization (`ROLE_USER` for `/user/**`, `ROLE_ADMIN` for `/admin/**`).
- Ownership verification ensures users can only inspect and cancel their own bookings.

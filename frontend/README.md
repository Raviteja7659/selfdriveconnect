# SelfDriveConnect — Frontend

A React (Vite) single-page app. It talks to a single **api-gateway** instance on port `8080`, which routes internally to all five backend services — the frontend itself no longer needs to know that five separate services even exist.

## Design

The visual identity is built around a repeated **license-plate badge** motif (mono font, bordered, rounded) used consistently for the nav logo, price tags, registration numbers, and seat-count chips — a small signature device tying the UI back to the subject matter instead of generic dashboard styling. The palette leans into road signage: asphalt charcoal for the nav/hero, a signal-yellow accent, highway green for success states, brick red for danger. Headings use Space Grotesk, body text uses Inter, and anything data-like (prices, plates, transaction IDs) uses JetBrains Mono.

## Setup

```bash
cd frontend
npm install
cp .env.example .env   # already done for you — points at the gateway on :8080
npm run dev
```

Runs on **http://localhost:5173**.

**The api-gateway (and, transitively, all five backend services) must already be running** — this is a pure client, it has no backend of its own. If a service is down, whichever feature depends on it will show an error inline rather than crashing the whole app.

## Important: CORS

The gateway's `application.yml` has a `globalcors` config allowing requests from `http://localhost:5173`. If you run the frontend on a different port, update that config (and restart the gateway) — otherwise the browser will block every request with a CORS error, even though the same calls work fine from Postman/curl/PowerShell (CORS is a browser-only protection; command-line tools don't enforce it).

## What's implemented

- **Auth** — combined login/register page, JWT stored in `localStorage`, decoded client-side (via `jwt-decode`) to read `userId`/`roles` without an extra round-trip
- **Browse & search** — home page lists all `ACTIVE` vehicles, searchable by city
- **Vehicle detail** — full info, reviews + average rating, and a booking form
- **Booking detail** — the hub page: shows payment history, and surfaces the right actions depending on who's viewing and the booking's status (pay, confirm, complete, cancel, refund, leave a review)
- **My bookings** — a renter's booking list
- **Owner dashboard** — tabs for "my vehicles" (with a "list a car" form) and "bookings on my vehicles"
- **Admin panel** — only visible/linked if the logged-in user's JWT contains the `ADMIN` role; lets an admin activate pending listings or suspend existing ones

## What's intentionally NOT built

- No offline/optimistic UI — every action re-fetches from the server after it completes, favoring correctness over snappiness. Fine for a portfolio demo.
- No pagination — `GET /api/v1/vehicles` returns everything; fine at demo scale, would need pagination on both ends at real scale.

## Folder structure

```
src/
  api/client.js          single gateway base URL, one axios factory reused for all five "clients" + shared error helper
  context/AuthContext.jsx JWT storage, decode, login/register/logout
  components/            Navbar, ProtectedRoute, VehicleCard, StatusBadge
  pages/                 Home, AuthPage, VehicleDetail, BookingDetail,
                          MyBookings, OwnerDashboard, AdminPanel
  index.css              design tokens + all component styles (no CSS-in-JS, no Tailwind)
```

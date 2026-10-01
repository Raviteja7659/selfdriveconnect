# SelfDriveConnect

A peer-to-peer self-drive car rental marketplace — connects car owners (supply) with renters (demand). Built as a Spring Boot microservices project, container-first, K8s-ready.

## Architecture (planned)

| Service | Port | Responsibility | Status |
|---|---|---|---|
| `vehicle-service` | 8086 | Car listings, availability, pricing, geospatial search | ✅ Built & JWT-secured |
| `user-service` | 8082 | Owner/renter accounts, auth (JWT issuer) | ✅ Built & running |
| `booking-service` | 8083 | Booking engine, event consumer, real-time push | ✅ Built & JWT-secured |
| `payment-service` | 8084 | Payment processing (mock gateway — free forever), event publisher | ✅ Built & running |
| `review-service` | 8085 | Ratings & reviews | ✅ Built & running |
| `api-gateway` | 8087 | Single entry point, routing, JWT pre-check | ✅ Built & running |
| `rabbitmq` | 5672 (AMQP) / 15672 (UI) | Event backbone — the Uber/Rapido-style piece | ✅ Built (this step) |
| `frontend` | 5174 | React SPA — the actual app UI | ✅ Built & running, fully containerized |

**Auth model:** `user-service` is the only service that issues JWTs (on register/login). `vehicle-service` and `booking-service` never issue tokens — they only *validate* ones already issued, using the same shared secret. This is a standard microservices pattern: one identity provider, many resource servers.

**Now fully containerized:** every service — including the frontend — has its own Dockerfile and runs via `docker-compose up`, with datasource URLs, inter-service URLs, and RabbitMQ host all overridden by environment variables rather than hardcoded. `localhost` defaults still work for running services individually outside Docker.

## Uber/Rapido-inspired architecture

SelfDriveConnect is a **self-drive rental marketplace**, not a ride-hailing service — there's no driver, no live in-trip GPS tracking, no dispatch/matching algorithm, no surge pricing. Those Uber/Rapido concepts don't apply here and weren't force-fit in. What genuinely does transfer is the *backend architecture pattern* behind them:

### 1. Geospatial search ("vehicles near me")

`vehicle-service` already stored `latitude`/`longitude` on every vehicle, but nothing used them until now. `GET /api/v1/vehicles/nearby?lat=&lng=&radiusKm=` runs the **Haversine formula** as a native SQL query — the same great-circle-distance calculation Uber/Rapido run to find nearby drivers, applied here to find nearby parked cars instead:

```sql
SELECT id, (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) *
    cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(latitude))
)) AS distance_km
FROM vehicles WHERE status = 'ACTIVE' ...
HAVING distance_km <= :radiusKm ORDER BY distance_km ASC
```

**Honest simplification:** at real scale, Uber/Rapido don't run this naively against every row — they index locations with geohashing, S2, or H3 cells (or PostGIS's R-tree index) so "nearby" is a fast index lookup, not a distance calculation against the whole table. A native Haversine query against a few hundred/thousand rows is genuinely fine for this project's scale; pretending it scales to millions of vehicles would be dishonest.

The frontend's "📍 Near me" button (on the vehicle browse page) uses the browser's geolocation API and renders results on a **Leaflet + OpenStreetMap** map — free, no API key, consistent with this project's "free forever" ethos.

### 2. Event-driven backbone (RabbitMQ)

Uber/Rapido lean heavily on event streaming (Kafka, at their scale) for cross-service coordination instead of only synchronous request/response calls — a trip status change fans out to matching, notifications, and analytics via events, not a chain of direct HTTP calls. This project adopts the same pattern with **RabbitMQ** (the right-sized tool here — a topic exchange, not a distributed log, since the throughput and retention needs are nowhere near Kafka's use case).

This directly replaced a real, previously-documented reliability gap: `payment-service` used to call `booking-service`'s internal endpoint directly (with a shared `X-Internal-Api-Key` header) to confirm a booking after a successful payment. If that call failed — network blip, `booking-service` restarting — the confirmation was silently lost, logged and forgotten, because the code couldn't roll back an already-successful charge just because a notification failed.

Now: `payment-service` publishes a `PaymentEvent` (routing key `payment.event`) to a shared `sdc.events` topic exchange. `booking-service` consumes it from its own bound queue and confirms the booking. If the listener throws, RabbitMQ redelivers the message once `booking-service` is healthy again — a real improvement, not just a different way of writing the same call. (It's not a *complete* fix — see "known gaps" below.)

`booking-service` also publishes its own `BookingLifecycleEvent` on every status change (create/confirm/cancel/complete), so any future consumer — a notification service sending emails, an analytics pipeline — can subscribe without `booking-service` needing to know they exist. That's the actual point of event-driven design: producers don't know their consumers.

**Known remaining gap:** this isn't a full outbox pattern. `payment-service` still publishes "best effort" after its own DB commit — there's a small window where a payment could succeed but the publish itself fails before RabbitMQ ever sees the message. A true outbox table (write the event to the same DB transaction as the payment, then a separate relay process publishes it with guaranteed delivery) would close even that gap.

### 3. Real-time push (Server-Sent Events)

Uber's live trip status (driver approaching, trip started, trip ended) updates the rider's screen without them refreshing. The equivalent here: when a booking's status changes, both the renter and the owner see a live toast notification pop up — "Booking confirmed", "New booking request" — without reloading the page.

Built with **Server-Sent Events**, not a full WebSocket broker, since this only needs one-way server-to-browser push. `booking-service` exposes `GET /api/v1/bookings/stream`; `BookingEventPublisher` pushes to any open connection for the relevant renter/owner every time it also publishes to RabbitMQ.

**A real, unavoidable wrinkle:** the browser's native `EventSource` API cannot attach custom headers, so it can't send `Authorization: Bearer <token>` like every other request in this project does. The standard workaround — a `token` query parameter instead — is checked only as a fallback when no header is present, both in `api-gateway`'s `JwtPreAuthFilter` and `booking-service`'s `JwtAuthFilter`.

## Step 1: Start local infra

```bash
cd SelfDriveConnect
docker-compose up -d
```

This starts:
- **PostgreSQL** on `localhost:5433` (user: `sdc_user`, pass: `sdc_pass`)
- **pgAdmin** on `localhost:5050` (login: `admin@sdc.com` / `admin`)
- **Redis** on `localhost:6379`

Create the databases for booking-service, user-service, payment-service, and review-service (one-time, since your Postgres volume predates the init script):
```bash
docker exec -it sdc-postgres psql -U sdc_user -d sdc_vehicle_db -c "CREATE DATABASE sdc_booking_db;"
docker exec -it sdc-postgres psql -U sdc_user -d sdc_vehicle_db -c "CREATE DATABASE sdc_user_db;"
docker exec -it sdc-postgres psql -U sdc_user -d sdc_vehicle_db -c "CREATE DATABASE sdc_payment_db;"
docker exec -it sdc-postgres psql -U sdc_user -d sdc_vehicle_db -c "CREATE DATABASE sdc_review_db;"
```

## Step 2: Run the five backend services

Each in its own terminal:

```bash
cd vehicle-service && mvn clean install && mvn spring-boot:run   # :8086
cd user-service && mvn clean install && mvn spring-boot:run      # :8082
cd booking-service && mvn clean install && mvn spring-boot:run   # :8083
cd payment-service && mvn clean install && mvn spring-boot:run   # :8084
cd review-service && mvn clean install && mvn spring-boot:run    # :8085
```

If any port conflicts with something already running on your machine, change `server.port` in that service's `application.yml` and let me know so I can keep everything in sync.

## Step 3: Run the API Gateway

```bash
cd api-gateway
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8087** — the single entry point for everything from here on. All five backend services above must already be running, since the gateway just routes to them; it doesn't do anything on its own.

## Step 4: Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on **http://localhost:5174**. Open that in your browser — this is the actual app now, not just an API you test with PowerShell. The frontend now talks to the gateway exclusively (one URL, `:8087`) instead of five separate service ports. See `frontend/README.md` for design notes and a full feature list.

Swagger UIs (still per-service, the gateway doesn't proxy these): `:8086/swagger-ui.html`, `:8082/swagger-ui.html`, `:8083/swagger-ui.html`, `:8084/swagger-ui.html`, `:8085/swagger-ui.html`

---

## Full end-to-end test (PowerShell) — register → list a car → book it

This walks through the whole platform as it now stands, with real auth throughout.

### 1. Register a user (gets you a JWT immediately)

```powershell
$register = @{
    fullName = "Ravi Teja"
    email = "ravi@example.com"
    password = "SecurePass123"
    phoneNumber = "9876543210"
    roles = @("OWNER", "RENTER")
} | ConvertTo-Json

$authResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/auth/register" -Method Post -Body $register -ContentType "application/json"
$token = $authResponse.token
$headers = @{ Authorization = "Bearer $token" }
$authResponse
```

(If you registered this user already in an earlier session, use login instead:)
```powershell
$login = @{ email = "ravi@example.com"; password = "SecurePass123" } | ConvertTo-Json
$authResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/auth/login" -Method Post -Body $login -ContentType "application/json"
$token = $authResponse.token
$headers = @{ Authorization = "Bearer $token" }
```

### 2. List a vehicle (requires auth — `ownerId` comes from your token, not the request body)

```powershell
$vehicle = @{
    brand = "Maruti Suzuki"
    model = "Swift"
    year = 2022
    registrationNumber = "TS09AB1234"
    fuelType = "PETROL"
    transmissionType = "MANUAL"
    seatingCapacity = 5
    pricePerHour = 150.00
    pricePerDay = 1800.00
    city = "Hyderabad"
    pickupLocation = "Gachibowli"
    latitude = 17.4400
    longitude = 78.3489
} | ConvertTo-Json

$newVehicle = Invoke-RestMethod -Uri "http://localhost:8086/api/v1/vehicles" -Method Post -Body $vehicle -ContentType "application/json" -Headers $headers
$newVehicle
```

Notice there's no `ownerId` field in the request anymore — vehicle-service reads it from your JWT. Try this same call with `$headers` removed and it will fail with `401/403`.

### 3. Activate the vehicle (admin-only action — see note below)

```powershell
Invoke-RestMethod -Uri "http://localhost:8086/api/v1/vehicles/$($newVehicle.id)/status?status=ACTIVE" -Method Patch -Headers $headers
```

**Note:** status changes are gated to users with the `ADMIN` role in the code. Since your test user only has `OWNER`/`RENTER`, this call will return `403 Forbidden` — that's the security working correctly. See the **Admin bootstrap & promotion** section below to create an admin account and get an `$adminHeaders` token, then re-run this step with `-Headers $adminHeaders` instead of `-Headers $headers`.

### 4. Search vehicles (public — no token needed)

```powershell
Invoke-RestMethod -Uri "http://localhost:8086/api/v1/vehicles/search?city=Hyderabad" -Method Get
```

### 5. Check availability (public)

```powershell
Invoke-RestMethod -Uri "http://localhost:8083/api/v1/bookings/availability?vehicleId=$($newVehicle.id)&start=2026-08-10T10:00:00&end=2026-08-10T18:00:00" -Method Get
```

### 6. Create a booking (requires auth — `renterId` comes from your token)

```powershell
$booking = @{
    vehicleId = $newVehicle.id
    startDateTime = "2026-08-10T10:00:00"
    endDateTime = "2026-08-10T18:00:00"
    pricingType = "HOURLY"
} | ConvertTo-Json

$newBooking = Invoke-RestMethod -Uri "http://localhost:8083/api/v1/bookings" -Method Post -Body $booking -ContentType "application/json" -Headers $headers
$newBooking
```

### 7. Try the same booking again — should 409 Conflict (double-booking prevention still works)

```powershell
Invoke-RestMethod -Uri "http://localhost:8083/api/v1/bookings" -Method Post -Body $booking -ContentType "application/json" -Headers $headers
```

### 8. Confirm / cancel / complete a booking

Since your test user is both `OWNER` and `RENTER` in this example, and happens to be the vehicle's owner, they can confirm and complete it. A different renter account could cancel it too.

```powershell
Invoke-RestMethod -Uri "http://localhost:8083/api/v1/bookings/$($newBooking.id)/confirm" -Method Patch -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8083/api/v1/bookings/$($newBooking.id)/complete" -Method Patch -Headers $headers
```

---

## Admin bootstrap & promotion

There's now a real mechanism for getting the first admin, instead of self-assigning `ADMIN` at registration (which is blocked).

### 1. Create the first admin (one-time only)

```powershell
$bootstrap = @{
    fullName = "Platform Admin"
    email = "admin@selfdriveconnect.com"
    password = "SuperSecureAdminPass123"
    phoneNumber = "9999999999"
    bootstrapSecret = "sdc-local-bootstrap-secret-change-me"
} | ConvertTo-Json

$adminAuth = Invoke-RestMethod -Uri "http://localhost:8082/api/v1/auth/bootstrap-admin" -Method Post -Body $bootstrap -ContentType "application/json"
$adminToken = $adminAuth.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
```

The secret above (`sdc-local-bootstrap-secret-change-me`) is the default in `application.yml` — change it there (or set the `ADMIN_BOOTSTRAP_SECRET` environment variable) before treating this as anything beyond a local demo.

**Try calling this endpoint a second time** — it will fail with `403 Forbidden` ("An admin already exists"), proving it can't be reused as a backdoor:
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/v1/auth/bootstrap-admin" -Method Post -Body $bootstrap -ContentType "application/json"
```

### 2. Now use the admin token to activate vehicles

```powershell
Invoke-RestMethod -Uri "http://localhost:8086/api/v1/vehicles/$($newVehicle.id)/status?status=ACTIVE" -Method Patch -Headers $adminHeaders
```

### 3. Promote a regular user to admin (or grant/adjust any roles)

Every admin after the first one is created this way — an existing admin promotes someone:

```powershell
$roleUpdate = @{ roles = @("OWNER", "RENTER", "ADMIN") } | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/v1/users/$($authResponse.userId)/roles" -Method Patch -Body $roleUpdate -ContentType "application/json" -Headers $adminHeaders
```

This uses `$authResponse.userId` from your earlier `ravi@example.com` registration — swap in whichever user id you want to promote. Try calling this same endpoint using `$headers` (your non-admin token) instead of `$adminHeaders` — it should fail with `403 Forbidden`.

---

## payment-service — free forever, mock gateway

This is the important part: **no real payment provider is involved anywhere in this service.** There's no Razorpay/Stripe account, no API key, no signup, and no way for it to ever move real money — because it doesn't talk to any external payment network at all. `MockPaymentGatewayService` just generates a fake transaction id and returns success (or, if you explicitly ask it to, a simulated failure). That's it. You can run this forever at zero cost and zero risk.

If you ever want a more realistic demo later, the note at the bottom of this section explains how to swap in Razorpay's free **test mode** (still $0, but calls a real sandbox API) — but that's optional and not required for anything here to work.

### Create its database (one-time)

```bash
docker exec -it sdc-postgres psql -U sdc_user -d sdc_vehicle_db -c "CREATE DATABASE sdc_payment_db;"
```

### Run it

```bash
cd payment-service
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8084**. Make sure `booking-service` (8083) is already running — payment-service calls it via Feign both to fetch booking details and to auto-confirm a booking after a successful payment.

### How the money flow works (all simulated)

1. **You create a booking** (`booking-service`, status `PENDING`) — same as before, nothing new here.
2. **You pay for it** (`POST /api/v1/payments` on payment-service):
   - payment-service fetches the booking from booking-service (forwarding your JWT automatically, so booking-service sees it's really you)
   - confirms you're the renter on that booking, and that it's still `PENDING` and unpaid
   - the amount is **never sent by you** — it's read from the booking's own `totalAmount`, same "never trust the client for money" principle as booking-service uses for pricing
   - runs it through the mock gateway → gets a fake `MOCK-<uuid>` transaction id
   - saves a `Payment` record with status `SUCCESS`
3. **On success, payment-service publishes a `PaymentEvent` to RabbitMQ, and booking-service auto-confirms the booking after consuming it** — this replaced an earlier design (a direct HTTP call using a shared `X-Internal-Api-Key` header) precisely because that call had no retry: if it failed, the confirmation was silently lost. RabbitMQ redelivers a message if the consumer fails to process it, closing most of that gap. See the **Uber/Rapido-inspired architecture** section above for the full reasoning.
4. **If you want to see a refund**, `PATCH /api/v1/payments/{id}/refund` (owner or admin only) marks the payment `REFUNDED` and asks booking-service to cancel the underlying booking.

### Test it (PowerShell)

Continuing from the earlier end-to-end flow — you should already have `$headers` (your token), `$newVehicle`, and `$newBooking` from before. If not, redo those steps first (making sure the vehicle is `ACTIVE` via `$adminHeaders`).

**1. Pay for the booking:**
```powershell
$payment = @{
    bookingId = $newBooking.id
    method = "UPI"
} | ConvertTo-Json

$paymentResult = Invoke-RestMethod -Uri "http://localhost:8084/api/v1/payments" -Method Post -Body $payment -ContentType "application/json" -Headers $headers
$paymentResult
```
You should see `status: SUCCESS` and a `transactionId` starting with `MOCK-`.

**2. Confirm the booking auto-confirmed:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8083/api/v1/bookings/$($newBooking.id)" -Method Get -Headers $headers
```
Its `status` should now be `CONFIRMED` — payment-service did this automatically, you didn't call booking-service's confirm endpoint yourself.

**3. Try paying for the SAME booking again — should 409 Conflict:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8084/api/v1/payments" -Method Post -Body $payment -ContentType "application/json" -Headers $headers
```

**4. Try a deliberately FAILED payment** (create a fresh booking first, since the previous one is now paid):
```powershell
$failPayment = @{
    bookingId = $newBooking.id
    method = "CARD"
    simulateFailure = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8084/api/v1/payments" -Method Post -Body $failPayment -ContentType "application/json" -Headers $headers
```
You'll see `status: FAILED` with a `failureReason`, and the booking stays `PENDING` (nothing gets confirmed on a failed payment). Note this will hit a 409 if you reuse a booking that already has a SUCCESS payment — use a fresh booking id for this test.

**5. Refund it (as the owner):**
```powershell
Invoke-RestMethod -Uri "http://localhost:8084/api/v1/payments/$($paymentResult.id)/refund" -Method Patch -Headers $headers
```
(Works here because your test user is both the renter *and* the vehicle owner. With separate accounts, you'd use the owner's own `$headers`.)

**6. View payment history:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8084/api/v1/payments/renter/$($authResponse.userId)" -Method Get -Headers $headers
```

### Note: swapping in a real (still-free) gateway later

If you ever want this to look even more realistic for a demo, Razorpay and Stripe both offer permanently-free **test/sandbox modes** — no real charges ever, but you get a real API integration to show off. That would mean replacing `MockPaymentGatewayService` with a real HTTP call to their test API using a test API key, while leaving all of `PaymentService`'s business logic (ownership checks, duplicate-payment prevention, the booking-confirm side effect) exactly as-is — that's precisely why the gateway logic was isolated into its own class in the first place.

---

## review-service

Ratings tied to actual completed rentals — not just "anyone can review any car."

### Create its database (one-time)

```bash
docker exec -it sdc-postgres psql -U sdc_user -d sdc_vehicle_db -c "CREATE DATABASE sdc_review_db;"
```

### Run it

```bash
cd review-service
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8085**. Needs `booking-service` (8083) running — review-service calls it via Feign to check the booking is `COMPLETED` and belongs to the caller.

### The rule this enforces

You can only review a booking that is:
1. **Yours** — you were the renter on it (checked against your JWT, not client input)
2. **`COMPLETED`** — not `PENDING`, `CONFIRMED`, or `CANCELLED`. You can't review a car rental you never finished.
3. **Not already reviewed** — one review per booking, ever (`bookingId` has a unique constraint).

Deleting a review is restricted to the review's own author (or an admin) — the vehicle's owner can't delete an unflattering review of their own car, which is the entire point of reviews being trustworthy.

### Test it (PowerShell)

This needs a `COMPLETED` booking. Continuing the earlier flow: you have `$newBooking` which should now be `CONFIRMED` (from the payment step). Complete it first, as the owner:

```powershell
Invoke-RestMethod -Uri "http://localhost:8083/api/v1/bookings/$($newBooking.id)/complete" -Method Patch -Headers $headers
```

**1. Leave a review:**
```powershell
$review = @{
    bookingId = $newBooking.id
    rating = 5
    comment = "Great car, smooth pickup, exactly as described."
} | ConvertTo-Json

$newReview = Invoke-RestMethod -Uri "http://localhost:8085/api/v1/reviews" -Method Post -Body $review -ContentType "application/json" -Headers $headers
$newReview
```

**2. Try reviewing the same booking again — should fail:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/reviews" -Method Post -Body $review -ContentType "application/json" -Headers $headers
```

**3. View a vehicle's reviews and rating summary (both public, no token needed):**
```powershell
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/reviews/vehicle/$($newVehicle.id)" -Method Get
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/reviews/vehicle/$($newVehicle.id)/summary" -Method Get
```
The summary returns `averageRating` and `totalReviews` — this is what a vehicle listing page would show next to a car's search result.

**4. Try reviewing a booking that ISN'T completed** (create a fresh booking, don't complete it, then try):
```powershell
$badReview = @{ bookingId = 999; rating = 3; comment = "test" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8085/api/v1/reviews" -Method Post -Body $badReview -ContentType "application/json" -Headers $headers
```
(Use a real booking id from your own testing that's still `PENDING`/`CONFIRMED` — you should get `422 Unprocessable Entity`.)

---

## api-gateway

One port instead of five, plus a centralized first line of defense against obviously-bad requests.

### What it actually does

1. **Routes by path prefix** — `/api/v1/auth/**` and `/api/v1/users/**` → user-service, `/api/v1/vehicles/**` → vehicle-service, `/api/v1/bookings/**` → booking-service, `/api/v1/payments/**` → payment-service, `/api/v1/reviews/**` → review-service. Declared in `application.yml`, not Java code — the standard Spring Cloud Gateway approach.
2. **Pre-checks JWTs for protected routes** — mirrors the exact same public/protected split each service already enforces individually (browsing vehicles/reviews and hitting `/auth/**` is public; everything else needs a valid bearer token). If the token is missing or garbage, the gateway rejects it with `401` before it even reaches a backend service.
3. **Centralizes CORS** — one `globalcors` config instead of duplicating the allowed-origins list across five services (though each service still has its own too, kept for direct-testing convenience).

### What it deliberately does NOT do

- **It doesn't replace each service's own auth checks.** The gateway's JWT check is shallow — signature valid, not expired, that's it. It doesn't decode roles or make ownership decisions ("is this really the vehicle's owner?"). Every service still does that itself. This is intentional defense-in-depth: never trust a single layer, especially not the edge layer, with authorization decisions.
- **Internal service-to-service calls bypass the gateway on purpose.** When payment-service calls booking-service (to fetch a booking, or to auto-confirm one after payment), that Feign call still goes directly to `booking-service:8083`, not through the gateway. Gateways are for external/browser traffic; routing internal calls through the edge would add a pointless hop and a false sense that "the gateway sees everything."

### Run it

```bash
cd api-gateway
mvn clean install
mvn spring-boot:run
```

Runs on **http://localhost:8087**. All five backend services must already be running.

### Test it (PowerShell) — same calls as before, now through one port

```powershell
# Public route through the gateway — works with no token
Invoke-RestMethod -Uri "http://localhost:8087/api/v1/vehicles/search?city=Hyderabad" -Method Get

# Protected route with NO token — gateway rejects it before booking-service ever sees it
Invoke-RestMethod -Uri "http://localhost:8087/api/v1/bookings/1" -Method Get
# -> 401 Unauthorized, "Missing bearer token"

# Protected route WITH a valid token — routes through to booking-service normally
Invoke-RestMethod -Uri "http://localhost:8087/api/v1/bookings/1" -Method Get -Headers $headers
```

You can also check what routes are registered:
```powershell
Invoke-RestMethod -Uri "http://localhost:8087/actuator/gateway/routes" -Method Get
```

---

## What's implemented so far

**vehicle-service**
- Full CRUD for vehicle listings, ownership enforced from JWT (not request body)
- Search by city (public, only returns `ACTIVE` status)
- Status lifecycle `PENDING_VERIFICATION → ACTIVE/INACTIVE/SUSPENDED`, restricted to `ADMIN`
- Update/delete restricted to the vehicle's owner or an `ADMIN`
- Registration number uniqueness check
- Swagger/OpenAPI docs, Actuator + Prometheus metrics

**user-service**
- Registration + login issuing JWTs (BCrypt-hashed passwords, never stored/returned in plaintext)
- Multi-role accounts (`RENTER`, `OWNER`, `ADMIN` — a user can hold more than one)
- `ADMIN` cannot be self-assigned at registration — blocked server-side
- One-time, self-disabling **admin bootstrap endpoint** protected by a server-side secret, for creating the platform's first admin
- Admin-only **role promotion endpoint** (`PATCH /users/{id}/roles`) — every admin after the first is created this way
- Stateless session (`SessionCreationPolicy.STATELESS`) — no server-side session state
- JWT embeds `userId` and `roles` as claims so other services never need to call back to user-service to authenticate a request

**booking-service**
- Cross-service validation via OpenFeign (checks vehicle-service that the vehicle is `ACTIVE`)
- Double-booking prevention via a single overlap-detection query (`startA < endB AND startB < endA`)
- Server-side pricing — client never sends `totalAmount`
- `renterId` derived from JWT, not request body
- Confirm/complete restricted to the vehicle's owner (or `ADMIN`); cancel allowed by either party
- Auto-confirms the booking on successful payment by consuming a `PaymentEvent` from RabbitMQ — replaces the old internal-API-key HTTP call with genuine retry/redelivery on failure
- Full status lifecycle: `PENDING → CONFIRMED → COMPLETED`, `CANCELLED` reachable from either

**payment-service**
- Fully mock payment gateway — zero external dependency, zero cost, zero real money movement, ever
- Amount always derived server-side from the booking's own price, never trusted from the client
- Duplicate-payment prevention (can't pay twice for the same booking)
- Ownership checks: only the renter on a booking can pay for it; only the owner (or admin) can refund
- Auto-confirms the booking on successful payment via a service-to-service call (separate auth mechanism — shared API key — from the normal user JWT flow)
- Explicit `simulateFailure` testing hook to exercise the failure path deterministically

**review-service**
- Reviews are tied to a specific `COMPLETED` booking, not just "renter reviewed vehicle" abstractly — enforced via a cross-service check to booking-service
- One review per booking, enforced with a unique constraint on `bookingId`
- Public read access (browsing a car's reviews/rating needs no login), write access requires auth + the completed-booking check
- Vehicle rating summary endpoint (average + count) — the aggregation a real listing page would show
- Deletion restricted to the review's own author or an admin — an owner can't delete their own bad reviews

**api-gateway**
- Single entry point (`:8087`) routing to all five backend services by path prefix
- Centralized JWT pre-check for protected routes — mirrors each service's own public/protected split, rejects missing/garbage tokens before they reach a backend
- Centralized CORS config — the frontend only needs to know about one origin now
- Deliberately shallow: real authorization (roles, ownership) still lives in each service; internal service-to-service Feign calls deliberately bypass the gateway

**frontend**
- React (Vite) SPA covering the full user journey: browse/search → vehicle detail with reviews → book → pay (mock gateway) → owner confirms/completes → renter reviews
- Now talks to a single gateway URL instead of five separate service ports — `api/client.js` has one shared axios factory, all five exported clients point at the same base URL
- Role-aware UI: the owner dashboard and admin panel only appear/function for accounts that actually hold those roles (checked from the JWT's `roles` claim)

## Next steps (build order)

1. Replace hardcoded service URLs (in both the gateway's `application.yml` and every Feign client) with **Eureka service discovery**
2. Harden the payment→booking confirmation step with an actual outbox/message-queue pattern (Kafka/RabbitMQ) instead of a best-effort synchronous call — see the comment in `PaymentService.confirmBookingAfterPayment` for the exact gap
3. Containerize everything, write K8s manifests, deploy to Minikube — move `jwt.secret`, `admin.bootstrap-secret`, and RabbitMQ credentials into K8s Secrets
4. Jenkins pipeline: build → test → docker push → deploy to Minikube
5. Frontend follow-ups: pagination on the vehicle list, a proper image upload flow (`imageUrl` currently has to be a plain URL), and an owner-facing "respond to review" feature

## Design decisions worth calling out in interviews

- **No cross-service foreign keys** — `ownerId`/`vehicleId`/`renterId` are plain reference IDs, not JPA relationships, because each service owns its own data (database-per-service pattern).
- **Single identity provider, many resource servers** — only `user-service` issues JWTs; `vehicle-service` and `booking-service` only validate them, using a shared HMAC secret. In production this would move to asymmetric signing (private key signs, public key verifies) so the signing secret itself never has to be distributed.
- **Trust nothing from the request body that identity should determine** — `ownerId` and `renterId` are deliberately absent from the create-vehicle/create-booking request DTOs. They're derived from the JWT server-side. This closes an obvious "act as anyone" exploit that existed before this step.
- **Authorization enforced at the service layer, not just the controller** — ownership checks (`AccessDeniedException`) live in `VehicleService`/`BookingService`, not just route-level Spring Security rules, so the business rule ("only the owner can edit their own listing") holds even if a new controller method is added later.
- **Status-gated search** — renters only see `ACTIVE` vehicles, mirroring real-world trust & safety requirements.
- **Server-side pricing** — booking-service never trusts a client-supplied price; it always recalculates from vehicle-service's live rates.
- **Overlap detection as a single query** — `startA < endB AND startB < endA` in JPQL pushes the conflict check to the DB rather than looping in Java.
- **Feign + explicit timeouts** — 3s connect / 5s read timeout on the vehicle-service call, so booking-service fails fast instead of hanging if vehicle-service is down.
- **Stateless JWT auth throughout** — no server-side session anywhere, which is what lets any of these services scale horizontally without sticky sessions.
- **Self-disabling bootstrap endpoint** — `bootstrap-admin` checks `existsByAdminRole()` before anything else, so it's a legitimate one-time setup step rather than a permanent backdoor. This is the same pattern real platforms use (e.g. Kubernetes' first-admin kubeconfig, Django's `createsuperuser`) — a privileged operation that's only valid in the "nothing exists yet" state.
- **Mock payment gateway isolated behind one interface** — `MockPaymentGatewayService` is the only class that knows payments aren't real. `PaymentService`'s business logic (ownership checks, duplicate-payment prevention, the confirm-on-success side effect) doesn't know or care whether the gateway is fake or real — swapping in Razorpay/Stripe test mode later means replacing one class, not rewriting the service.
- **Two different trust boundaries for two different kinds of caller** — user-facing endpoints trust a JWT (proves "this is a specific logged-in person"); the payment→booking confirmation call trusts a shared API key instead (proves "this is a specific *service*, acting on the platform's own authority, not impersonating any user"). Conflating these two would be a real security smell — a user JWT should never be treated as authorization for a service-level operation like "bypass the normal confirm-only-by-owner rule."
- **Payment succeeding and booking confirming are treated as separate failure domains, on purpose** — if the confirm call fails after a successful charge, the payment record still stands as `SUCCESS` rather than being rolled back, because the charge is the source of truth for "did the renter actually pay." The comment in `PaymentService.confirmBookingAfterPayment` calls out that the honest fix is an outbox/event-queue pattern, not a synchronous best-effort call — worth raising unprompted in an interview as "here's the corner I cut and why."
- **Reviews require proof of a completed transaction, not just an opinion** — `review-service` doesn't let anyone review any vehicle; it checks with booking-service that the caller actually completed that specific rental first. This is the same trust principle as e-commerce "verified purchase" reviews, and it's enforced with a real cross-service call, not just a client-side assumption.
- **Aggregation lives in the read path, not stored redundantly** — `VehicleRatingSummary` computes the average on read (`AVG(rating)` in JPQL) rather than maintaining a cached `averageRating` column on the vehicle that could drift out of sync. For this scale that's the right tradeoff; a high-traffic version would cache it and invalidate on new reviews.
- **The gateway's JWT check is shallow on purpose** — it verifies signature and expiry, nothing else. It doesn't decode roles, doesn't make "can this person do this" decisions, and every service downstream still runs its own full check independently. Putting authorization logic at the edge and trusting it everywhere downstream is a common real-world mistake; this project deliberately avoids it in favor of defense-in-depth, even though it means the same JWT gets parsed multiple times per request chain.
- **Internal service-to-service traffic bypasses the gateway** — Feign calls between services (payment→booking, review→booking) go straight to the target service's own port, not through `:8087`. Gateways exist to mediate *external* traffic; routing internal calls through the edge adds a hop for no benefit and can create a false mental model that "the gateway sees everything," which it doesn't and shouldn't need to.

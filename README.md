# BookShare 2.0

> **Demo checkout notice:** BookShare currently includes a simulated purchase flow for demonstration/testing only. It does not connect to Razorpay or any payment provider, does not collect or transfer real money, and should not be used for real purchases. A confirmed demo checkout records a `DEMO-...` reference and marks the listing as SOLD.


**An evidence-backed community book lending & exchange platform.**

Java Spring Boot + PostgreSQL + React, with a whimsical "library in the woods" interface.

Members of a physical community (building, hostel, neighbourhood) put the real books they own into a shared library.

Every exchange is tracked from request to return, photos at pickup and return protect both sides, reports go through

evidence-based review instead of automatic punishment, and every member shows a star rating **and** a separate

on-time-return record.

---------------------------------------------------------------------------------------------------

## 1. Run it

### Option A: Docker Compose (recommended, one command)

cp .env.example .env        # optional, change the secrets

docker compose up --build

Open **http://localhost:3000**. nginx serves the React app and proxies /api, /uploads and /ws to the backend.

### Option B: local development (Windows PostgreSQL installation)

This is the local setup used for development. Spring Boot connects to the PostgreSQL
server installed on Windows and managed through PostgreSQL/pgAdmin. It does not use
the PostgreSQL container defined in Docker Compose.

Database connection
Setting	Value
Database name	bookshare
Host	localhost
Port	5432
Username	postgres
Password	The password configured for your local PostgreSQL postgres user


Make sure the PostgreSQL Windows service is running and that the bookshare database
exists before starting the backend. To verify the active connection in pgAdmin, run:
SELECT current_database(), current_user, inet_server_port();
Expected values: database bookshare, user postgres, port 5432.

Start the application (Git Bash on Windows)

Open three terminals from the project root (bookshare/).
# Terminal 1 — Backend (Java 17+, Maven 3.9+)
cd backend
../apache-maven-3.9.16-bin/apache-maven-3.9.16/bin/mvn spring-boot:run
Backend URL: http://localhost:8080.
# Terminal 2 — Frontend (Node.js 18+)
cd frontend
npm install
npm run dev
Frontend URL: http://localhost:5173. Vite proxies /api, /uploads and /ws
to the backend on port 8080.

Credentials: configure the local PostgreSQL password in
backend/src/main/resources/application.yml or through the environment variable used
by that file. Do not publish your real database password, .env files, JWT secrets,
or other credentials on GitHub.

### Accounts created on first start

| Role | Email | Password |

|---|---|---|

| Admin | admin@bookshare.local | Admin@12345 |

| Reader (Hollow Hall Hostel) | asha@demo.local | Demo@1234 |

| Reader (Hollow Hall Hostel) | rohan@demo.local | Demo@1234 |

| Reader (Lantern Lane Apartments) | meera@demo.local | Demo@1234 |

Set SEED_DEMO=false to skip the demo readers, books and rooms (the community tree and the admin are always created).

The community tree is *Evergreen City > Mossgate > {Hollow Hall Hostel, Lantern Lane Apartments}*.

---------------------------------------------------------------------------------------------------

## 2. Local setup and verification

The project has been run locally with this development setup:
- Backend: Spring Boot starts successfully at http://localhost:8080.
- Database: the backend connects to the locally installed PostgreSQL database
  bookshare on localhost:5432 using the postgres database user. Hibernate is
  configured with ddl-auto: update.
- Frontend: the React/Vite development server runs at http://localhost:5173.
  Vite proxies API, upload and WebSocket requests to the backend.
- Application flow: user registration and login have been tested in the local setup.
- Not implemented: optional YOLO damage detection (only a hook, see section 7).
  Payments are excluded on purpose.
- There are no automated tests. Use the checklist in section 9 for further manual testing.
Docker Compose is included as an optional containerized setup. Its PostgreSQL service
is separate from the PostgreSQL server installed on Windows. Data stored in one database
will not automatically appear in the other.

---------------------------------------------------------------------------------------------------

## 3. Features, mapped to the proposal's steps

| Step | Feature | Backend | Frontend page |

|---|---|---|---|

| 1 | Accounts, virtual library, search & filters (genre, community, listing type, condition, availability) | AuthService, BookService | Register, Login, Library, Add book, Book detail |

| 2 | Borrow/return state machine | BorrowService | My Exchanges, Exchange detail |

| 3 | Condition verification (3 to 5 photos at listing, pickup, return; transitions blocked without them) | ConditionCheckService | Photo picker in Add book / Exchange detail |

| 4 | 1-to-1 chat scoped to a request or sale listing, real time over STOMP | ChatService, StompAuthInterceptor | Chat box on Exchange / Sale pages |

| 5 | Community hierarchy Building > Neighbourhood > City, widen search | CommunityService | "Search in" selector, Admin |

| 6 | Evidence-backed reports + escalation ladder + admin review | ReportService, TrustService, AdminService | Report dialog, Admin |

| 7 | Star rating **and** separate compliance ratio with raw counts | RatingService, ReputationService | Profile, Trust card (shown before the owner accepts) |

| 8 | Buy & Sell: list, chat, reserve, mark sold, **no payments** | SaleService | Marketplace, Sale detail |

| 9 | Community chatrooms & book clubs (time-boxed), same-community only | RoomService | Book Clubs |

| 10 | Filters + "readers who borrowed this also borrowed" (co-occurrence, no ML) | RecommendationService | Home, Book detail |

| 11 | Reading rooms: schedule, capacity, text chat, optional WebRTC audio/video | ReadingRoomService, SignalingController | Reading Rooms |

| 12 | Docker packaging + admin dashboard (pending reports with photos, escalation status, active sales) | docker-compose.yml, AdminService | Admin |

### Borrow lifecycle (only these transitions are allowed)

| From | To | Who | Gate |

|---|---|---|---|

| PENDING | ACCEPTED / REJECTED | owner | owner not under review; book free; return_due_at = now + lending days |

| ACCEPTED | PICKED_UP | owner | a **PICKUP** ConditionCheck exists; book becomes unavailable |

| PICKED_UP | BORROWED | borrower | confirms receipt |

| BORROWED | RETURN_DUE | automatic (every 15 min) | due date passed; reminder posted in the chat |

| BORROWED / RETURN_DUE | RETURNED | borrower | a **RETURN** ConditionCheck exists; book becomes available |

| RETURNED | COMPLETED | owner | confirms; unlocks the two-way rating |

### Trust and reporting

* A report starts as SUBMITTED and **never punishes by itself**. The listing, pickup and return photos are attached automatically.

* An admin sets it to UNDER_REVIEW, VALID or INVALID. Only VALID reports count.

* Escalation per reported user: **1 valid** = warning, **2** = cannot borrow for restriction-days (14), can still lend, **3 or more** = account under admin review. Admins can also set any status manually.

* NOT_RETURNED can only be filed by the owner, when the loan is RETURN_DUE, a reminder was sent, and grace-days (5) have passed since the due date.

* Other categories (DAMAGED, MISSING_PAGES, OTHER) can be filed by either party once the book is RETURNED or COMPLETED.

### Reputation (two separate numbers, computed from immutable history)

* **Stars:** average of communication, care and reliability ratings received. One rating per direction per completed exchange.

* **On-time returns:** on-time returns / resolved borrows (picked up and either returned or already overdue).

* **Completed lends:** completed lends / accepted lend requests.

* The UI always shows raw counts next to the percentage (for example *9/10 on time*), so one late return cannot mislead on a tiny sample.

### Real-time channels (STOMP over WebSocket at /ws)

The JWT is sent in the CONNECT frame; every SUBSCRIBE is authorised by ChatAccessService.

Messages are sent over REST (persisted, then broadcast), so a refresh always shows the full history.

| Topic | Who may subscribe |

|---|---|

| /topic/request.{id} | the borrower and the owner |

| /topic/sale.{id}.{buyerId} | the seller and that buyer |

| /topic/room.{id} | members of the room's community |

| /topic/reading.{id} | members of the reading room's community |

| /topic/reading.{id}.signal | WebRTC signalling (offer, answer, ice, join, leave), sent to /app/reading/{id}/signal |

---------------------------------------------------------------------------------------------------

## 4. REST API (all under /api, JWT Authorization: Bearer ... except where noted)

| Area | Endpoints |

|---|---|

| Auth | POST /auth/register, POST /auth/login (public), GET /auth/me |

| Communities | GET /communities/public (public), GET /communities, POST /communities |

| Users | GET /users/{id}/profile, PUT /users/me |

| Books | GET /books?q&genre&condition&listingType&available&scope=own\|parent\|root, GET /books/mine, GET/PUT/DELETE /books/{id}, POST /books (multipart: book JSON, cover, photos), GET/POST /books/{id}/condition-checks, GET /books/{id}/recommendations, GET /recommendations |

| Exchanges | POST /requests, GET /requests?role=borrower\|owner, GET /requests/{id}, POST /requests/{id}/accept\|reject\|pickup\|receive\|return\|complete, POST /requests/{id}/condition-checks?stage=PICKUP\|RETURN (multipart photos), GET/POST /requests/{id}/reports, POST /requests/{id}/ratings |

| Chat | GET/POST /chat/request/{id}/messages, GET /chat/sale/{id}/messages?buyerId=, POST /chat/sale/{id}/messages, GET/POST /chat/room/{id}/messages, GET/POST /chat/reading/{id}/messages |

| Sales | GET /sales, GET /sales/mine, GET /sales/{id}, POST /sales, POST /sales/{id}/interest, GET /sales/{id}/threads, PUT /sales/{id}/reserve\|release\|sold\|cancel |

| Rooms | GET/POST /rooms, GET /rooms/{id}, GET/POST /reading-rooms, GET /reading-rooms/{id}, POST /reading-rooms/{id}/join\|leave, DELETE /reading-rooms/{id} |

| Admin (role ADMIN) | GET /admin/stats, GET /admin/reports?status=OPEN\|ALL\|VALID\|INVALID, PUT /admin/reports/{id}, GET /admin/flagged-users, PUT /admin/users/{id}/status, GET /admin/sales |

---------------------------------------------------------------------------------------------------

## 5. Configuration (environment variables)

| Variable | Default | Meaning |

|---|---|---|

| DB_URL, DB_USER, DB_PASSWORD | local URL jdbc:postgresql://localhost:5432/bookshare, user postgres, and your local DB password | database connection; Docker Compose supplies separate values for its own database container |

| JWT_SECRET | placeholder | **change it**; at least 32 bytes |

| STORAGE_TYPE | local | local (folder UPLOAD_DIR) or cloudinary |

| CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET | empty | needed when STORAGE_TYPE=cloudinary |

| ADMIN_EMAIL, ADMIN_PASSWORD | admin@bookshare.local / Admin@12345 | admin created on first start |

| SEED_DEMO | true | create demo readers, books and rooms |

| CORS_ORIGINS | localhost origins | comma-separated allowed origins |

| app.trust.grace-days, app.trust.restriction-days | 5, 14 | in application.yml |

---------------------------------------------------------------------------------------------------

## 6. Additions and deviations from the proposal (so nothing is a surprise)

1. **`Room` and ReadingRoom entities** were added for Steps 9 and 11 (they are in the deck's ER diagram, in orange).

2. **`Message.thread_user_id`**: a sale listing can attract several buyers, so each buyer gets their own thread with the seller.

3. **Reading rooms are scoped to the host's community** (the proposal does not say). Chatrooms are same-community only, as Step 9 requires.

4. **Two-step hand-over and return confirmation:** the owner marks "handed over" and the borrower confirms receipt; the borrower marks "returned" and the owner confirms. This gives meaning to PICKED_UP vs BORROWED and to RETURNED vs COMPLETED.

5. **"Total borrows" in the compliance ratio** counts picked-up books that were returned or are overdue, so a book that is still out and not yet due does not lower the ratio.

6. **Account status UNDER_REVIEW** pauses both borrowing and lending until an admin decides; RESTRICTED pauses only borrowing.

7. **Sold books are archived** and disappear from the library and the marketplace. Ownership is not transferred in the system.

---------------------------------------------------------------------------------------------------

## 7. Optional extensions (not built)

* **YOLO damage detection:** implement service/DamageDetector as a Spring bean (for example calling a Python/ONNX service).

  Its note is saved in ConditionCheck.aiNotes for pickup/return checks and shown next to the photos. With no bean, nothing happens.

  This needs your own labelled dataset of damaged books.

* **WebRTC** uses a public Google STUN server and a peer-to-peer mesh, fine for a handful of readers. Behind strict NATs you need a TURN server (add it in frontend/src/hooks.js, constant ICE).

  Browsers only allow the microphone and camera on https\:// or http\://localhost.

* **Payments** are excluded by design. Prices are informational and settled in person.

---------------------------------------------------------------------------------------------------

## 8. Project layout

bookshare/

├── docker-compose.yml  .env.example  README.md

├── backend/   (Spring Boot 3.3, Java 17, Maven)

│   └── src/main/java/com/bookshare/

│       ├── domain/    entities and enums

│       ├── repo/      Spring Data JPA repositories

│       ├── service/   business rules (borrow state machine, trust, reputation, chat, sales, ...)

│       ├── web/       REST controllers, DTOs, error handling

│       ├── security/  JWT service and filter

│       ├── config/    security, CORS, WebSocket, uploads

│       └── ws/        STOMP authentication and WebRTC signalling

└── frontend/  (React 18, Vite, React Router, STOMP client)

    └── src/ pages/ components/ hooks.js api.js auth.jsx styles.css

## 9. Manual test checklist (from the proposal's "how you'll know it works")

1. **Step 1:** sign in as Asha and Rohan (same hostel). Library shows both books; filters by genre, listing type and condition narrow the list.

2. **Step 2:** Rohan requests Asha's *The Hobbit*; Asha accepts; status moves along the trail. Availability flips to "On loan" at hand-over and back after return.

3. **Step 3:** try "Mark returned" before uploading return photos. The API answers 409. Upload 3 to 5 photos and it succeeds; photos appear in the three-column comparison.

4. **Step 4:** open the exchange in two browsers (Asha and Rohan). Messages appear instantly and survive a refresh.

5. **Step 5:** Meera (other building) searches with "My building" (sees only her own), then "My neighbourhood" (also sees the hostel's books).

6. **Step 6:** after a return, file a *Damaged* report, then as admin mark it valid. Check the user goes ACTIVE > WARNED > RESTRICTED > UNDER_REVIEW across 1, 2, 3 valid reports. Filing *Not returned* before the grace period returns 409.

7. **Step 7:** complete two exchanges with different ratings and one late return; stars and on-time ratio change independently on the profile.

8. **Step 8:** list a book for sale, message from another account, reserve, mark sold; it disappears from the marketplace.

9. **Steps 9 to 11:** open a book club room (not visible to other communities); join a reading room in two browsers and start audio or video.

10. **Step 12:** docker compose up --build on a clean machine, then open the Admin page.
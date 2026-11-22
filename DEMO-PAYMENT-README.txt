BOOKSHARE — SIMULATED PAYMENT FEATURE

IMPORTANT: This is a visual/application demo only. It does NOT integrate Razorpay, collect real money, or transfer money to sellers. Do not describe a simulated transaction as a real payment.

WHAT CHANGED
- Added POST /api/sales/{id}/simulate-payment (authenticated buyer only).
- Validates buyer is not seller, listing is still available, reserved listing belongs to buyer, and book has a positive price.
- Records a DEMO-... reference, SIMULATED_SUCCESS status and timestamp.
- Sets sale listing to SOLD, sets book unavailable and archived, and posts a system chat message.
- Updated SaleDetail, BookDetail and Marketplace copy/buttons to identify the flow as demo-only.
- No Razorpay credentials or dependencies are required.

APPLY
This archive contains the full project. Back up your current project first, then use this folder as your updated project or copy the changed files into the matching paths. Keep your local application.yml/database credentials and .env files safe; do not publish secrets. Hibernate ddl-auto=update should add the three new nullable demo payment columns to sale_listings.

RUN
Backend: from backend/ run your existing Maven spring-boot:run command.
Frontend: from frontend/ run npm install (if needed), then npm run dev.

TEST
1. Log in as a buyer account different from the seller.
2. Open an available Marketplace listing.
3. Click “Simulate payment” and confirm the browser prompt.
4. Verify the listing becomes SOLD, the book is archived, and the seller receives a system chat message.
5. Check sale_listings.demo_payment_reference, demo_payment_status and demo_paid_at in PostgreSQL.

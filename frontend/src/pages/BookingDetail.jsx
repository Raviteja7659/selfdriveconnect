import { useEffect, useState, useCallback } from 'react'
import { useParams, Link } from 'react-router-dom'
import { bookingApi, paymentApi, reviewApi, vehicleApi, extractErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'
import StatusBadge from '../components/StatusBadge'

export default function BookingDetail() {
  const { id } = useParams()
  const { user } = useAuth()

  const [booking, setBooking] = useState(null)
  const [vehicle, setVehicle] = useState(null)
  const [payments, setPayments] = useState([])
  const [alreadyReviewed, setAlreadyReviewed] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [actionMsg, setActionMsg] = useState('')
  const [busy, setBusy] = useState(false)

  const [paymentForm, setPaymentForm] = useState({ method: 'UPI', simulateFailure: false })
  const [reviewForm, setReviewForm] = useState({ rating: 5, comment: '' })

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const { data: bookingData } = await bookingApi.get(`/api/v1/bookings/${id}`)
      setBooking(bookingData)

      const [vehicleRes, paymentsRes] = await Promise.all([
        vehicleApi.get(`/api/v1/vehicles/${bookingData.vehicleId}`),
        paymentApi.get(`/api/v1/payments/booking/${id}`),
      ])
      setVehicle(vehicleRes.data)
      setPayments(paymentsRes.data)

      if (user?.userId === bookingData.renterId) {
        const { data: myReviews } = await reviewApi.get(`/api/v1/reviews/renter/${user.userId}`)
        setAlreadyReviewed(myReviews.some((r) => r.bookingId === Number(id)))
      }
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [id, user])

  useEffect(() => {
    load()
  }, [load])

  const isRenter = user?.userId === booking?.renterId
  const isOwner = user?.userId === booking?.ownerId
  const successfulPayment = payments.find((p) => p.status === 'SUCCESS')

  async function runAction(fn) {
    setActionError('')
    setActionMsg('')
    setBusy(true)
    try {
      await fn()
      await load()
    } catch (err) {
      setActionError(extractErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  function handlePay(e) {
    e.preventDefault()
    runAction(async () => {
      await paymentApi.post('/api/v1/payments', {
        bookingId: Number(id),
        method: paymentForm.method,
        simulateFailure: paymentForm.simulateFailure,
      })
      setActionMsg(paymentForm.simulateFailure ? 'Payment failed (simulated).' : 'Payment successful — booking confirmed.')
    })
  }

  function handleConfirm() {
    runAction(async () => {
      await bookingApi.patch(`/api/v1/bookings/${id}/confirm`)
      setActionMsg('Booking confirmed.')
    })
  }

  function handleComplete() {
    runAction(async () => {
      await bookingApi.patch(`/api/v1/bookings/${id}/complete`)
      setActionMsg('Booking marked as completed.')
    })
  }

  function handleCancel() {
    runAction(async () => {
      await bookingApi.patch(`/api/v1/bookings/${id}/cancel`, null, { params: { reason: 'Cancelled from app' } })
      setActionMsg('Booking cancelled.')
    })
  }

  function handleRefund() {
    runAction(async () => {
      await paymentApi.patch(`/api/v1/payments/${successfulPayment.id}/refund`)
      setActionMsg('Payment refunded and booking cancelled.')
    })
  }

  function handleReview(e) {
    e.preventDefault()
    runAction(async () => {
      await reviewApi.post('/api/v1/reviews', {
        bookingId: Number(id),
        rating: Number(reviewForm.rating),
        comment: reviewForm.comment,
      })
      setActionMsg('Review submitted — thanks!')
      setAlreadyReviewed(true)
    })
  }

  if (loading) return <div className="spinner-text">Loading booking…</div>
  if (error) {
    return (
      <div className="container page">
        <div className="alert alert--error">{error}</div>
      </div>
    )
  }
  if (!booking || !vehicle) return null

  return (
    <div className="container page">
      <div className="page-header">
        <Link to={`/vehicles/${vehicle.id}`} className="muted">
          ← Back to {vehicle.brand} {vehicle.model}
        </Link>
      </div>

      <div className="detail-grid">
        <div className="card">
          <div className="spread" style={{ marginBottom: 6 }}>
            <h2>
              {vehicle.brand} {vehicle.model}
            </h2>
            <StatusBadge status={booking.status} />
          </div>
          <p className="muted">{vehicle.registrationNumber} · {vehicle.city}</p>

          <hr className="divider" />

          <div className="stat-row">
            <div className="stat">
              <b>{new Date(booking.startDateTime).toLocaleString()}</b>
              <span>Start</span>
            </div>
            <div className="stat">
              <b>{new Date(booking.endDateTime).toLocaleString()}</b>
              <span>End</span>
            </div>
          </div>

          <div className="stat-row" style={{ marginTop: 16 }}>
            <div className="stat">
              <b>₹{booking.totalAmount}</b>
              <span>{booking.pricingType}</span>
            </div>
            <div className="stat">
              <b>{isRenter ? 'You' : isOwner ? 'Someone else' : '—'}</b>
              <span>Renter</span>
            </div>
          </div>

          {payments.length > 0 && (
            <>
              <hr className="divider" />
              <h3 style={{ fontSize: 15, marginBottom: 10 }}>Payment history</h3>
              <div className="stack">
                {payments.map((p) => (
                  <div key={p.id} className="row spread">
                    <span className="muted">
                      {p.method} · {p.transactionId || p.failureReason}
                    </span>
                    <StatusBadge status={p.status} />
                  </div>
                ))}
              </div>
            </>
          )}
        </div>

        <div className="card stack">
          {actionMsg && <div className="alert alert--success">{actionMsg}</div>}
          {actionError && <div className="alert alert--error">{actionError}</div>}

          {isRenter && booking.status === 'PENDING' && !successfulPayment && (
            <form onSubmit={handlePay}>
              <h3 style={{ marginBottom: 12 }}>Pay for this booking</h3>
              <div className="field">
                <label className="label">Payment method</label>
                <select
                  className="input"
                  value={paymentForm.method}
                  onChange={(e) => setPaymentForm({ ...paymentForm, method: e.target.value })}
                >
                  <option value="UPI">UPI</option>
                  <option value="CARD">Card</option>
                  <option value="NETBANKING">Netbanking</option>
                  <option value="WALLET">Wallet</option>
                </select>
              </div>
              <label className="checkbox-item" style={{ marginBottom: 14 }}>
                <input
                  type="checkbox"
                  checked={paymentForm.simulateFailure}
                  onChange={(e) => setPaymentForm({ ...paymentForm, simulateFailure: e.target.checked })}
                />
                Simulate a failed payment (testing)
              </label>
              <button className="btn btn-primary btn-block" disabled={busy}>
                {busy ? 'Processing…' : 'Pay now (mock gateway — free)'}
              </button>
            </form>
          )}

          {isOwner && booking.status === 'PENDING' && (
            <button className="btn btn-dark btn-block" onClick={handleConfirm} disabled={busy}>
              Confirm booking manually
            </button>
          )}

          {isOwner && booking.status === 'CONFIRMED' && (
            <button className="btn btn-dark btn-block" onClick={handleComplete} disabled={busy}>
              Mark as completed
            </button>
          )}

          {(isRenter || isOwner) && ['PENDING', 'CONFIRMED'].includes(booking.status) && (
            <button className="btn btn-danger btn-block" onClick={handleCancel} disabled={busy}>
              Cancel booking
            </button>
          )}

          {isOwner && successfulPayment && ['CONFIRMED', 'COMPLETED'].includes(booking.status) && (
            <button className="btn btn-ghost btn-block" onClick={handleRefund} disabled={busy}>
              Refund payment &amp; cancel booking
            </button>
          )}

          {isRenter && booking.status === 'COMPLETED' && !alreadyReviewed && (
            <form onSubmit={handleReview}>
              <h3 style={{ marginBottom: 12 }}>Leave a review</h3>
              <div className="field">
                <label className="label">Rating</label>
                <select
                  className="input"
                  value={reviewForm.rating}
                  onChange={(e) => setReviewForm({ ...reviewForm, rating: e.target.value })}
                >
                  {[5, 4, 3, 2, 1].map((n) => (
                    <option key={n} value={n}>
                      {'★'.repeat(n)} ({n})
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label className="label">Comment</label>
                <textarea
                  className="input"
                  rows={3}
                  value={reviewForm.comment}
                  onChange={(e) => setReviewForm({ ...reviewForm, comment: e.target.value })}
                />
              </div>
              <button className="btn btn-primary btn-block" disabled={busy}>
                {busy ? 'Submitting…' : 'Submit review'}
              </button>
            </form>
          )}

          {isRenter && booking.status === 'COMPLETED' && alreadyReviewed && (
            <p className="muted">You've already reviewed this rental — thanks!</p>
          )}

          {!isRenter && !isOwner && <p className="muted">You're not a party to this booking.</p>}
        </div>
      </div>
    </div>
  )
}

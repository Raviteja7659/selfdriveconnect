import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { vehicleApi, bookingApi, reviewApi, extractErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function VehicleDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  const [vehicle, setVehicle] = useState(null)
  const [reviews, setReviews] = useState([])
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [bookingForm, setBookingForm] = useState({
    startDateTime: '',
    endDateTime: '',
    pricingType: 'HOURLY',
  })
  const [bookingError, setBookingError] = useState('')
  const [bookingLoading, setBookingLoading] = useState(false)

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError('')
      try {
        const [vehicleRes, reviewsRes, summaryRes] = await Promise.all([
          vehicleApi.get(`/api/v1/vehicles/${id}`),
          reviewApi.get(`/api/v1/reviews/vehicle/${id}`),
          reviewApi.get(`/api/v1/reviews/vehicle/${id}/summary`),
        ])
        setVehicle(vehicleRes.data)
        setReviews(reviewsRes.data)
        setSummary(summaryRes.data)
      } catch (err) {
        setError(extractErrorMessage(err))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  async function handleBook(e) {
    e.preventDefault()
    setBookingError('')

    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/vehicles/${id}` } })
      return
    }

    if (!bookingForm.startDateTime || !bookingForm.endDateTime) {
      setBookingError('Choose a start and end time.')
      return
    }

    setBookingLoading(true)
    try {
      const { data } = await bookingApi.post('/api/v1/bookings', {
        vehicleId: Number(id),
        startDateTime: bookingForm.startDateTime,
        endDateTime: bookingForm.endDateTime,
        pricingType: bookingForm.pricingType,
      })
      navigate(`/bookings/${data.id}`)
    } catch (err) {
      setBookingError(extractErrorMessage(err))
    } finally {
      setBookingLoading(false)
    }
  }

  if (loading) return <div className="spinner-text">Loading vehicle…</div>
  if (error) {
    return (
      <div className="container page">
        <div className="alert alert--error">{error}</div>
      </div>
    )
  }
  if (!vehicle) return null

  return (
    <div className="container page">
      <div className="detail-grid">
        <div>
          <div className="card" style={{ marginBottom: 20 }}>
            <div className="spread" style={{ alignItems: 'flex-start' }}>
              <div>
                <h2>
                  {vehicle.brand} {vehicle.model} <span className="muted">· {vehicle.year}</span>
                </h2>
                <p className="muted" style={{ marginTop: 4 }}>
                  {vehicle.pickupLocation ? `${vehicle.pickupLocation}, ` : ''}
                  {vehicle.city}
                </p>
              </div>
              <span className="plate">{vehicle.registrationNumber}</span>
            </div>

            <div className="stat-row">
              <div className="stat">
                <b>₹{vehicle.pricePerHour}</b>
                <span>per hour</span>
              </div>
              <div className="stat">
                <b>₹{vehicle.pricePerDay}</b>
                <span>per day</span>
              </div>
              <div className="stat">
                <b>{vehicle.seatingCapacity}</b>
                <span>seats</span>
              </div>
              <div className="stat">
                <b>{vehicle.transmissionType}</b>
                <span>{vehicle.fuelType}</span>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="spread">
              <h3>Reviews</h3>
              {summary && summary.totalReviews > 0 && (
                <div className="rating">
                  <span className="rating__stars">★ {summary.averageRating}</span>
                  <span>({summary.totalReviews})</span>
                </div>
              )}
            </div>

            {reviews.length === 0 ? (
              <p className="muted" style={{ marginTop: 12 }}>
                No reviews yet — be the first to rent and review this car.
              </p>
            ) : (
              <div style={{ marginTop: 8 }}>
                {reviews.map((r) => (
                  <div key={r.id} className="review-item">
                    <div className="row" style={{ marginBottom: 4 }}>
                      <span className="rating__stars">{'★'.repeat(r.rating)}</span>
                      <span className="muted">{new Date(r.createdAt).toLocaleDateString()}</span>
                    </div>
                    {r.comment && <p style={{ margin: 0, fontSize: 14 }}>{r.comment}</p>}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="card">
          <h3 style={{ marginBottom: 4 }}>Book this car</h3>
          <p className="muted" style={{ marginBottom: 16 }}>
            Pricing is calculated automatically from the owner's rates.
          </p>

          {vehicle.status !== 'ACTIVE' ? (
            <div className="alert alert--error">
              This vehicle isn't currently bookable (status: {vehicle.status}).
            </div>
          ) : (
            <form onSubmit={handleBook}>
              {bookingError && <div className="alert alert--error">{bookingError}</div>}

              <div className="field">
                <label className="label">Pricing</label>
                <select
                  className="input"
                  value={bookingForm.pricingType}
                  onChange={(e) => setBookingForm({ ...bookingForm, pricingType: e.target.value })}
                >
                  <option value="HOURLY">Hourly</option>
                  <option value="DAILY">Daily</option>
                </select>
              </div>

              <div className="field">
                <label className="label">Start</label>
                <input
                  className="input"
                  type="datetime-local"
                  required
                  value={bookingForm.startDateTime}
                  onChange={(e) => setBookingForm({ ...bookingForm, startDateTime: e.target.value })}
                />
              </div>

              <div className="field">
                <label className="label">End</label>
                <input
                  className="input"
                  type="datetime-local"
                  required
                  value={bookingForm.endDateTime}
                  onChange={(e) => setBookingForm({ ...bookingForm, endDateTime: e.target.value })}
                />
              </div>

              <button className="btn btn-primary btn-block" disabled={bookingLoading}>
                {bookingLoading ? 'Checking availability…' : isAuthenticated ? 'Request to book' : 'Log in to book'}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}

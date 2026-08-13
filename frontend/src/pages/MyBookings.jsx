import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { bookingApi, vehicleApi, extractErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'
import StatusBadge from '../components/StatusBadge'

export default function MyBookings() {
  const { user } = useAuth()
  const [bookings, setBookings] = useState([])
  const [vehicles, setVehicles] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError('')
      try {
        const { data } = await bookingApi.get(`/api/v1/bookings/renter/${user.userId}`)
        setBookings(data.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)))

        const vehicleIds = [...new Set(data.map((b) => b.vehicleId))]
        const vehicleResults = await Promise.all(
          vehicleIds.map((vid) => vehicleApi.get(`/api/v1/vehicles/${vid}`).then((r) => r.data))
        )
        const map = {}
        vehicleResults.forEach((v) => (map[v.id] = v))
        setVehicles(map)
      } catch (err) {
        setError(extractErrorMessage(err))
      } finally {
        setLoading(false)
      }
    }
    if (user?.userId) load()
  }, [user])

  return (
    <div className="container page">
      <div className="page-header">
        <h2>My bookings</h2>
        <p>Everything you've booked as a renter.</p>
      </div>

      {error && <div className="alert alert--error">{error}</div>}

      {loading ? (
        <div className="spinner-text">Loading…</div>
      ) : bookings.length === 0 ? (
        <div className="empty-state">
          No bookings yet. <Link to="/">Browse cars</Link> to get started.
        </div>
      ) : (
        <div className="stack">
          {bookings.map((b) => {
            const v = vehicles[b.vehicleId]
            return (
              <Link key={b.id} to={`/bookings/${b.id}`} className="card" style={{ display: 'block' }}>
                <div className="spread">
                  <div>
                    <div style={{ fontWeight: 600, fontFamily: 'var(--font-display)' }}>
                      {v ? `${v.brand} ${v.model}` : `Vehicle #${b.vehicleId}`}
                    </div>
                    <div className="muted" style={{ marginTop: 4 }}>
                      {new Date(b.startDateTime).toLocaleDateString()} → {new Date(b.endDateTime).toLocaleDateString()}
                      {' · ₹'}
                      {b.totalAmount}
                    </div>
                  </div>
                  <StatusBadge status={b.status} />
                </div>
              </Link>
            )
          })}
        </div>
      )}
    </div>
  )
}

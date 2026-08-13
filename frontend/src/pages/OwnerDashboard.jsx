import { useEffect, useState, useCallback } from 'react'
import { vehicleApi, bookingApi, extractErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'
import StatusBadge from '../components/StatusBadge'
import { Link } from 'react-router-dom'

const emptyForm = {
  brand: '',
  model: '',
  year: new Date().getFullYear(),
  registrationNumber: '',
  fuelType: 'PETROL',
  transmissionType: 'MANUAL',
  seatingCapacity: 5,
  pricePerHour: '',
  pricePerDay: '',
  city: '',
  pickupLocation: '',
}

export default function OwnerDashboard() {
  const { user } = useAuth()
  const [tab, setTab] = useState('vehicles')
  const [myVehicles, setMyVehicles] = useState([])
  const [ownerBookings, setOwnerBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [form, setForm] = useState(emptyForm)
  const [formError, setFormError] = useState('')
  const [formLoading, setFormLoading] = useState(false)
  const [showForm, setShowForm] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [vehiclesRes, bookingsRes] = await Promise.all([
        vehicleApi.get(`/api/v1/vehicles/owner/${user.userId}`),
        bookingApi.get(`/api/v1/bookings/owner/${user.userId}`),
      ])
      setMyVehicles(vehiclesRes.data)
      setOwnerBookings(bookingsRes.data.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)))
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    if (user?.userId) load()
  }, [user, load])

  async function handleAddVehicle(e) {
    e.preventDefault()
    setFormError('')
    setFormLoading(true)
    try {
      await vehicleApi.post('/api/v1/vehicles', {
        ...form,
        year: Number(form.year),
        seatingCapacity: Number(form.seatingCapacity),
        pricePerHour: Number(form.pricePerHour),
        pricePerDay: Number(form.pricePerDay),
      })
      setForm(emptyForm)
      setShowForm(false)
      await load()
    } catch (err) {
      setFormError(extractErrorMessage(err))
    } finally {
      setFormLoading(false)
    }
  }

  if (!user?.roles?.includes('OWNER')) {
    return (
      <div className="container page">
        <div className="alert alert--error">
          Your account isn't registered as an owner. Contact an admin to have the OWNER role added, or register a
          new account with "List my car" checked.
        </div>
      </div>
    )
  }

  return (
    <div className="container page">
      <div className="page-header spread">
        <div>
          <h2>My vehicles</h2>
          <p>Manage your listings and see bookings made against them.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm((s) => !s)}>
          {showForm ? 'Cancel' : '+ List a car'}
        </button>
      </div>

      {error && <div className="alert alert--error">{error}</div>}

      {showForm && (
        <div className="card" style={{ marginBottom: 24 }}>
          <h3 style={{ marginBottom: 14 }}>New listing</h3>
          <form onSubmit={handleAddVehicle}>
            {formError && <div className="alert alert--error">{formError}</div>}
            <div className="form-grid">
              <div className="field">
                <label className="label">Brand</label>
                <input className="input" required value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} />
              </div>
              <div className="field">
                <label className="label">Model</label>
                <input className="input" required value={form.model} onChange={(e) => setForm({ ...form, model: e.target.value })} />
              </div>
              <div className="field">
                <label className="label">Year</label>
                <input className="input" type="number" required value={form.year} onChange={(e) => setForm({ ...form, year: e.target.value })} />
              </div>
              <div className="field">
                <label className="label">Registration number</label>
                <input
                  className="input"
                  required
                  value={form.registrationNumber}
                  onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })}
                />
              </div>
              <div className="field">
                <label className="label">Fuel type</label>
                <select className="input" value={form.fuelType} onChange={(e) => setForm({ ...form, fuelType: e.target.value })}>
                  <option>PETROL</option>
                  <option>DIESEL</option>
                  <option>ELECTRIC</option>
                  <option>CNG</option>
                  <option>HYBRID</option>
                </select>
              </div>
              <div className="field">
                <label className="label">Transmission</label>
                <select
                  className="input"
                  value={form.transmissionType}
                  onChange={(e) => setForm({ ...form, transmissionType: e.target.value })}
                >
                  <option>MANUAL</option>
                  <option>AUTOMATIC</option>
                </select>
              </div>
              <div className="field">
                <label className="label">Seating capacity</label>
                <input
                  className="input"
                  type="number"
                  required
                  value={form.seatingCapacity}
                  onChange={(e) => setForm({ ...form, seatingCapacity: e.target.value })}
                />
              </div>
              <div className="field">
                <label className="label">City</label>
                <input className="input" required value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
              </div>
              <div className="field">
                <label className="label">Price per hour (₹)</label>
                <input
                  className="input"
                  type="number"
                  step="0.01"
                  required
                  value={form.pricePerHour}
                  onChange={(e) => setForm({ ...form, pricePerHour: e.target.value })}
                />
              </div>
              <div className="field">
                <label className="label">Price per day (₹)</label>
                <input
                  className="input"
                  type="number"
                  step="0.01"
                  required
                  value={form.pricePerDay}
                  onChange={(e) => setForm({ ...form, pricePerDay: e.target.value })}
                />
              </div>
              <div className="field" style={{ gridColumn: '1 / -1' }}>
                <label className="label">Pickup location</label>
                <input
                  className="input"
                  value={form.pickupLocation}
                  onChange={(e) => setForm({ ...form, pickupLocation: e.target.value })}
                />
              </div>
            </div>
            <p className="muted" style={{ marginBottom: 14 }}>
              New listings start as <StatusBadge status="PENDING_VERIFICATION" /> — an admin needs to activate them
              before renters can find or book them.
            </p>
            <button className="btn btn-primary" disabled={formLoading}>
              {formLoading ? 'Listing…' : 'List this car'}
            </button>
          </form>
        </div>
      )}

      <div className="auth-tabs" style={{ maxWidth: 320, marginBottom: 20 }}>
        <div className={`auth-tab ${tab === 'vehicles' ? 'auth-tab--active' : ''}`} onClick={() => setTab('vehicles')}>
          Vehicles ({myVehicles.length})
        </div>
        <div className={`auth-tab ${tab === 'bookings' ? 'auth-tab--active' : ''}`} onClick={() => setTab('bookings')}>
          Bookings ({ownerBookings.length})
        </div>
      </div>

      {loading ? (
        <div className="spinner-text">Loading…</div>
      ) : tab === 'vehicles' ? (
        myVehicles.length === 0 ? (
          <div className="empty-state">You haven't listed a car yet.</div>
        ) : (
          <div className="vehicle-grid">
            {myVehicles.map((v) => (
              <Link key={v.id} to={`/vehicles/${v.id}`} className="vehicle-card">
                <div className="vehicle-card__media">{v.registrationNumber}</div>
                <div className="vehicle-card__body">
                  <div className="vehicle-card__title">
                    {v.brand} {v.model}
                  </div>
                  <div className="vehicle-card__meta">{v.city}</div>
                  <div className="vehicle-card__footer">
                    <span className="price-tag">
                      ₹{v.pricePerHour}
                      <span> /hr</span>
                    </span>
                    <StatusBadge status={v.status} />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )
      ) : ownerBookings.length === 0 ? (
        <div className="empty-state">No one has booked your cars yet.</div>
      ) : (
        <div className="stack">
          {ownerBookings.map((b) => (
            <Link key={b.id} to={`/bookings/${b.id}`} className="card" style={{ display: 'block' }}>
              <div className="spread">
                <div>
                  <div style={{ fontWeight: 600 }}>Vehicle #{b.vehicleId}</div>
                  <div className="muted" style={{ marginTop: 4 }}>
                    {new Date(b.startDateTime).toLocaleDateString()} → {new Date(b.endDateTime).toLocaleDateString()}
                    {' · ₹'}
                    {b.totalAmount}
                  </div>
                </div>
                <StatusBadge status={b.status} />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

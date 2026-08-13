import { useEffect, useState } from 'react'
import { vehicleApi, extractErrorMessage } from '../api/client'
import VehicleCard from '../components/VehicleCard'

export default function Home() {
  const [city, setCity] = useState('')
  const [vehicles, setVehicles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [searched, setSearched] = useState(false)

  async function loadAll() {
    setLoading(true)
    setError('')
    try {
      const { data } = await vehicleApi.get('/api/v1/vehicles')
      setVehicles(data.filter((v) => v.status === 'ACTIVE'))
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAll()
  }, [])

  async function handleSearch(e) {
    e.preventDefault()
    if (!city.trim()) {
      setSearched(false)
      loadAll()
      return
    }
    setLoading(true)
    setError('')
    setSearched(true)
    try {
      const { data } = await vehicleApi.get('/api/v1/vehicles/search', { params: { city } })
      setVehicles(data)
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <div className="hero">
        <div className="container">
          <div className="hero__eyebrow">peer-to-peer · self-drive · pay only for the miles you need</div>
          <h1>Rent a car from someone down the road, not a rental counter.</h1>
          <p>
            Search by city, book by the hour or the day, and skip the queue — every car here belongs to a real
            owner nearby.
          </p>
          <form className="search-bar" onSubmit={handleSearch}>
            <input
              className="input"
              placeholder="Search by city — e.g. Hyderabad"
              value={city}
              onChange={(e) => setCity(e.target.value)}
            />
            <button className="btn btn-primary" type="submit">
              Find a ride
            </button>
          </form>
        </div>
      </div>

      <div className="container page">
        <div className="page-header spread">
          <div>
            <h2>{searched ? `Cars in ${city}` : 'Available now'}</h2>
            <p>{loading ? 'Loading…' : `${vehicles.length} car${vehicles.length === 1 ? '' : 's'} ready to book`}</p>
          </div>
        </div>

        {error && <div className="alert alert--error">{error}</div>}

        {loading ? (
          <div className="spinner-text">Loading vehicles…</div>
        ) : vehicles.length === 0 ? (
          <div className="empty-state">
            No active listings {searched ? `in ${city}` : ''} yet. Check back soon, or try another city.
          </div>
        ) : (
          <div className="vehicle-grid">
            {vehicles.map((v) => (
              <VehicleCard key={v.id} vehicle={v} />
            ))}
          </div>
        )}
      </div>
    </>
  )
}

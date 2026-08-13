import { useEffect, useState, useCallback } from 'react'
import { vehicleApi, extractErrorMessage } from '../api/client'
import StatusBadge from '../components/StatusBadge'

export default function AdminPanel() {
  const [vehicles, setVehicles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await vehicleApi.get('/api/v1/vehicles')
      setVehicles(data)
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  async function updateStatus(id, status) {
    setBusyId(id)
    setError('')
    try {
      await vehicleApi.patch(`/api/v1/vehicles/${id}/status`, null, { params: { status } })
      await load()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  const pending = vehicles.filter((v) => v.status === 'PENDING_VERIFICATION')
  const others = vehicles.filter((v) => v.status !== 'PENDING_VERIFICATION')

  return (
    <div className="container page">
      <div className="page-header">
        <h2>Admin — vehicle verification</h2>
        <p>Activate new listings so renters can find and book them, or suspend problematic ones.</p>
      </div>

      {error && <div className="alert alert--error">{error}</div>}

      {loading ? (
        <div className="spinner-text">Loading…</div>
      ) : (
        <>
          <h3 style={{ marginBottom: 12 }}>Pending verification ({pending.length})</h3>
          {pending.length === 0 ? (
            <p className="muted" style={{ marginBottom: 28 }}>
              Nothing waiting on review.
            </p>
          ) : (
            <div className="stack" style={{ marginBottom: 28 }}>
              {pending.map((v) => (
                <div key={v.id} className="card spread">
                  <div>
                    <div style={{ fontWeight: 600 }}>
                      {v.brand} {v.model} <span className="plate">{v.registrationNumber}</span>
                    </div>
                    <div className="muted" style={{ marginTop: 4 }}>
                      Owner #{v.ownerId} · {v.city}
                    </div>
                  </div>
                  <div className="row">
                    <button
                      className="btn btn-primary btn-sm"
                      disabled={busyId === v.id}
                      onClick={() => updateStatus(v.id, 'ACTIVE')}
                    >
                      Activate
                    </button>
                    <button
                      className="btn btn-danger btn-sm"
                      disabled={busyId === v.id}
                      onClick={() => updateStatus(v.id, 'SUSPENDED')}
                    >
                      Reject
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          <h3 style={{ marginBottom: 12 }}>All other listings ({others.length})</h3>
          <div className="stack">
            {others.map((v) => (
              <div key={v.id} className="card spread">
                <div>
                  <div style={{ fontWeight: 600 }}>
                    {v.brand} {v.model} <span className="plate">{v.registrationNumber}</span>
                  </div>
                  <div className="muted" style={{ marginTop: 4 }}>
                    Owner #{v.ownerId} · {v.city}
                  </div>
                </div>
                <div className="row">
                  <StatusBadge status={v.status} />
                  {v.status === 'ACTIVE' && (
                    <button
                      className="btn btn-ghost btn-sm"
                      disabled={busyId === v.id}
                      onClick={() => updateStatus(v.id, 'SUSPENDED')}
                    >
                      Suspend
                    </button>
                  )}
                  {v.status === 'SUSPENDED' && (
                    <button
                      className="btn btn-ghost btn-sm"
                      disabled={busyId === v.id}
                      onClick={() => updateStatus(v.id, 'ACTIVE')}
                    >
                      Reactivate
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function AuthPage() {
  const [tab, setTab] = useState('login')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const redirectTo = location.state?.from || '/'

  const [loginForm, setLoginForm] = useState({ email: '', password: '' })
  const [regForm, setRegForm] = useState({
    fullName: '',
    email: '',
    password: '',
    phoneNumber: '',
    roles: { RENTER: true, OWNER: false },
  })

  async function handleLogin(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    const result = await login(loginForm)
    setLoading(false)
    if (result.ok) navigate(redirectTo)
    else setError(result.message)
  }

  async function handleRegister(e) {
    e.preventDefault()
    setError('')
    const selectedRoles = Object.entries(regForm.roles)
      .filter(([, checked]) => checked)
      .map(([role]) => role)

    if (selectedRoles.length === 0) {
      setError('Select at least one role: renter or owner.')
      return
    }

    setLoading(true)
    const result = await register({ ...regForm, roles: selectedRoles })
    setLoading(false)
    if (result.ok) navigate(redirectTo)
    else setError(result.message)
  }

  return (
    <div className="page">
      <div className="container">
        <div className="auth-shell">
          <div className="auth-tabs">
            <div
              className={`auth-tab ${tab === 'login' ? 'auth-tab--active' : ''}`}
              onClick={() => {
                setTab('login')
                setError('')
              }}
            >
              Log in
            </div>
            <div
              className={`auth-tab ${tab === 'register' ? 'auth-tab--active' : ''}`}
              onClick={() => {
                setTab('register')
                setError('')
              }}
            >
              Register
            </div>
          </div>

          <div className="card">
            {error && <div className="alert alert--error">{error}</div>}

            {tab === 'login' ? (
              <form onSubmit={handleLogin}>
                <div className="field">
                  <label className="label">Email</label>
                  <input
                    className="input"
                    type="email"
                    required
                    value={loginForm.email}
                    onChange={(e) => setLoginForm({ ...loginForm, email: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label className="label">Password</label>
                  <input
                    className="input"
                    type="password"
                    required
                    value={loginForm.password}
                    onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                  />
                </div>
                <button className="btn btn-primary btn-block" disabled={loading}>
                  {loading ? 'Logging in…' : 'Log in'}
                </button>
              </form>
            ) : (
              <form onSubmit={handleRegister}>
                <div className="field">
                  <label className="label">Full name</label>
                  <input
                    className="input"
                    required
                    value={regForm.fullName}
                    onChange={(e) => setRegForm({ ...regForm, fullName: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label className="label">Email</label>
                  <input
                    className="input"
                    type="email"
                    required
                    value={regForm.email}
                    onChange={(e) => setRegForm({ ...regForm, email: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label className="label">Password (min 8 characters)</label>
                  <input
                    className="input"
                    type="password"
                    required
                    minLength={8}
                    value={regForm.password}
                    onChange={(e) => setRegForm({ ...regForm, password: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label className="label">Phone number</label>
                  <input
                    className="input"
                    value={regForm.phoneNumber}
                    onChange={(e) => setRegForm({ ...regForm, phoneNumber: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label className="label">I want to</label>
                  <div className="checkbox-row">
                    <label className="checkbox-item">
                      <input
                        type="checkbox"
                        checked={regForm.roles.RENTER}
                        onChange={(e) =>
                          setRegForm({ ...regForm, roles: { ...regForm.roles, RENTER: e.target.checked } })
                        }
                      />
                      Rent cars
                    </label>
                    <label className="checkbox-item">
                      <input
                        type="checkbox"
                        checked={regForm.roles.OWNER}
                        onChange={(e) =>
                          setRegForm({ ...regForm, roles: { ...regForm.roles, OWNER: e.target.checked } })
                        }
                      />
                      List my car
                    </label>
                  </div>
                </div>
                <button className="btn btn-primary btn-block" disabled={loading}>
                  {loading ? 'Creating account…' : 'Create account'}
                </button>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

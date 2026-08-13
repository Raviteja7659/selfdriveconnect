import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <nav className="nav">
      <div className="nav__inner">
        <Link to="/" className="nav__logo">
          <span className="plate">SDC</span>
          SelfDriveConnect
        </Link>

        <div className="nav__links">
          <Link to="/" className="nav__link">
            Browse
          </Link>

          {isAuthenticated && (
            <>
              <Link to="/my-bookings" className="nav__link">
                My bookings
              </Link>
              <Link to="/owner" className="nav__link">
                My vehicles
              </Link>
              {user?.roles?.includes('ADMIN') && (
                <Link to="/admin" className="nav__link">
                  Admin
                </Link>
              )}
            </>
          )}
        </div>

        <div className="nav__user">
          {isAuthenticated ? (
            <>
              <span className="nav__email">{user?.email}</span>
              <button className="btn btn-ghost btn-sm" onClick={handleLogout} style={{ color: '#fff', borderColor: '#3a4048' }}>
                Log out
              </button>
            </>
          ) : (
            <Link to="/login">
              <button className="btn btn-primary btn-sm">Log in</button>
            </Link>
          )}
        </div>
      </div>
    </nav>
  )
}

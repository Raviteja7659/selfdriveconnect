import { createContext, useContext, useState, useCallback } from 'react'
import { jwtDecode } from 'jwt-decode'
import { userApi, extractErrorMessage } from '../api/client'

const AuthContext = createContext(null)

function decodeUser(token) {
  if (!token) return null
  try {
    const payload = jwtDecode(token)
    return {
      userId: payload.userId,
      email: payload.sub,
      roles: (payload.roles || []).map((r) => r.replace('ROLE_', '')),
    }
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('sdc_token'))
  const [user, setUser] = useState(() => decodeUser(localStorage.getItem('sdc_token')))

  const applyToken = useCallback((newToken) => {
    localStorage.setItem('sdc_token', newToken)
    setToken(newToken)
    setUser(decodeUser(newToken))
  }, [])

  const register = useCallback(
    async ({ fullName, email, password, phoneNumber, roles }) => {
      try {
        const { data } = await userApi.post('/api/v1/auth/register', {
          fullName,
          email,
          password,
          phoneNumber,
          roles,
        })
        applyToken(data.token)
        return { ok: true }
      } catch (err) {
        return { ok: false, message: extractErrorMessage(err) }
      }
    },
    [applyToken]
  )

  const login = useCallback(
    async ({ email, password }) => {
      try {
        const { data } = await userApi.post('/api/v1/auth/login', { email, password })
        applyToken(data.token)
        return { ok: true }
      } catch (err) {
        return { ok: false, message: extractErrorMessage(err) }
      }
    },
    [applyToken]
  )

  const logout = useCallback(() => {
    localStorage.removeItem('sdc_token')
    setToken(null)
    setUser(null)
  }, [])

  const value = { token, user, isAuthenticated: !!token, register, login, logout }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

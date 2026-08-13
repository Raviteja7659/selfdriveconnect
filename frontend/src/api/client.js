import axios from 'axios'

// All five services now sit behind api-gateway on a single port.
// Every exported client below points at the SAME base URL — kept as
// separate names (userApi, vehicleApi, ...) purely so the rest of the
// app didn't need to change when the gateway was introduced. The
// gateway routes by path prefix (see api-gateway/application.yml),
// so e.g. vehicleApi.get('/api/v1/vehicles') and
// bookingApi.get('/api/v1/bookings/1') both go to :8087 and get
// forwarded to the right backend service from there.
const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL || 'http://cv1211.online:8087'

function createClient(baseURL) {
  const client = axios.create({ baseURL })

  // Every service that requires auth validates the SAME JWT (issued
  // only by user-service), so one interceptor covers all five clients.
  client.interceptors.request.use((config) => {
    const token = localStorage.getItem('sdc_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  return client
}

export const userApi = createClient(GATEWAY_URL)
export const vehicleApi = createClient(GATEWAY_URL)
export const bookingApi = createClient(GATEWAY_URL)
export const paymentApi = createClient(GATEWAY_URL)
export const reviewApi = createClient(GATEWAY_URL)

/**
 * Every backend error handler in this project returns the same shape:
 * { timestamp, status, error, message, fieldErrors? }. This pulls the
 * best human-readable message out of that shape (or axios's own
 * fallback) so components don't each need to know the error format.
 */
export function extractErrorMessage(err) {
  const data = err?.response?.data
  if (!data) return err?.message || 'Something went wrong. Please try again.'
  if (data.fieldErrors) {
    const first = Object.values(data.fieldErrors)[0]
    return first || data.message || 'Please check the form and try again.'
  }
  return data.message || 'Something went wrong. Please try again.'
}

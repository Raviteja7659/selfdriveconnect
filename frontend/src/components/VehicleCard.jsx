import { Link } from 'react-router-dom'

export default function VehicleCard({ vehicle }) {
  return (
    <Link to={`/vehicles/${vehicle.id}`} className="vehicle-card">
      <div className="vehicle-card__media">{vehicle.registrationNumber}</div>
      <div className="vehicle-card__body">
        <div className="vehicle-card__title">
          {vehicle.brand} {vehicle.model}
        </div>
        <div className="vehicle-card__meta">
          {vehicle.city} · {vehicle.transmissionType?.toLowerCase()} · {vehicle.fuelType?.toLowerCase()}
        </div>
        <div className="vehicle-card__footer">
          <div className="price-tag">
            ₹{vehicle.pricePerHour}
            <span> /hr</span>
          </div>
          <span className="plate" style={{ fontSize: 11 }}>
            {vehicle.seatingCapacity} seats
          </span>
        </div>
      </div>
    </Link>
  )
}

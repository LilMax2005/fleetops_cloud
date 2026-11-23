// Contratos de servicio-flota (fleetops-openapi.json) y de los modulos EP5.
export type TripStatus = 'SCHEDULED' | 'IN_TRANSIT' | 'DELAYED' | 'INCIDENT' | 'COMPLETED' | 'CANCELLED';
export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type IncidentType = 'MECHANICAL_FAILURE' | 'ROUTE_DEVIATION' | 'PROLONGED_STOP' | 'GENERAL';

export interface Place { name: string; latitude: number; longitude: number; }

export interface Trip {
  id: string;
  origin: Place;
  destination: Place;
  driver: { id: string; name: string };
  vehicle: { id: string; plate: string };
  status: TripStatus;
  departureTime: string;
  estimatedArrival: string;
  progress: number;
  totalDistanceKm: number;
  travelledDistanceKm: number;
}

export interface VehiclePosition {
  vehicleId: string;
  tripId?: string;
  latitude: number;
  longitude: number;
  speedKph: number;
  fuelPercent: number;
  odometerKm: number;
  recordedAt: string;
  source?: string;
}

export interface Alert {
  id: number;
  tripId: string;
  vehicleId: string;
  type: 'SPEEDING' | 'LOW_FUEL' | string;
  severity: AlertSeverity;
  title: string;
  description: string;
  status: 'OPEN' | 'RESOLVED';
  detectedAt: string;
}

export interface TripDetail extends Trip {
  lastPosition?: VehiclePosition;
  speedLimitKph: number;
  lowFuelThresholdPercent: number;
  alerts: Alert[];
  timeline: { label: string; state: string }[];
}

export interface Telemetry {
  eventId: string;
  vehicleId: string;
  tripId?: string;
  latitude: number;
  longitude: number;
  speedKph: number;
  fuelPercent: number;
  odometerKm: number;
  recordedAt: string;
  source?: string;
}

export interface CreateIncident {
  tripId: string;
  vehicleId: string;
  type: IncidentType;
  title: string;
  description: string;
  severity: AlertSeverity;
  latitude?: number;
  longitude?: number;
}

export interface AnalyticsSummary {
  tripsTracked: number;
  tripsEnRoute: number;
  tripsCompleted: number;
  totalDelays: number;
  totalIncidents: number;
  avgTripDurationMinutes: number;
  vehiclesReporting: number;
  telemetryReadings: number;
  alertsRaised: number;
}

export interface VehicleStats {
  vehicleId: string;
  readings: number;
  avgSpeedKmh: number;
  maxSpeedKmh: number;
  lastFuelLevel?: number;
  lastReadingAt?: string;
  alertsRaised: number;
}

export interface TripMetric {
  tripId: string;
  vehicleId: string;
  vehiclePlate?: string;
  status: string;
  events: number;
  delays: number;
  incidents: number;
  durationMinutes?: number;
  lastEventAt?: string;
}

export interface TopicSummary {
  name: string;
  partitions: number;
  replicationFactor: number;
}

export interface TopicDescription extends TopicSummary {
  internal: boolean;
  partitionInfo: { partition: number; leader: number; replicas: number[]; isr: number[] }[];
  configs: Record<string, string>;
}

export interface CreateTopic {
  name: string;
  partitions: number;
  replicationFactor: number;
  configs: Record<string, string>;
}

export interface ConsumerGroupDetail {
  groupId: string;
  state: string;
  coordinator: string;
  assignor: string;
  members: { memberId: string; clientId: string; host: string; assignment: string[] }[];
  offsets: { topic: string; partition: number; committedOffset: number; endOffset: number; lag: number }[];
  totalLag: number;
}

export type Row = Record<string, unknown>;

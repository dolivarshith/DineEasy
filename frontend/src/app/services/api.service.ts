import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

/**
 * Interface representing the current authenticated user session details.
 */
export interface UserSession {
  username: string;
  email: string;
  role: string; // "ADMIN" or "CUSTOMER"
}

/**
 * Interface representing a dining table entity details.
 */
export interface DiningTable {
  id?: number;
  tableNumber: number;
  capacity: number;
  status: string; // "AVAILABLE" or "RESERVED"
}

/**
 * Interface representing a table reservation details.
 */
export interface Reservation {
  id?: number;
  diningTable: DiningTable;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  reservationDate: string; // Format: YYYY-MM-DD
  reservationTime: string; // Format: HH:MM:SS
  numberOfGuests: number;
  status: string; // "PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"
  specialRequests?: string;
}

/**
 * Interface representing admin dashboard statistics response.
 */
export interface Stats {
  totalTables: number;
  activeReservationsToday: number;
  pendingReservationsCount: number;
  totalReservations: number;
}

/**
 * Core API service handling all HTTP communication between the Angular frontend and Spring Boot backend.
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {
  // Base URL pointing to the Spring Boot REST API
  private apiUrl = 'http://localhost:8080/api';
  
  // Angular Signal to track current user state reactively across components
  public currentUser = signal<UserSession | null>(null);

  constructor(private http: HttpClient) {
    // Attempt to reload the session from localStorage on application startup
    if (typeof localStorage !== 'undefined' && typeof localStorage.getItem === 'function') {
      try {
        const savedUser = localStorage.getItem('dineease_user');
        if (savedUser) {
          this.currentUser.set(JSON.parse(savedUser));
        }
      } catch (e) {
        // [DEBUG BREAKPOINT: Break here if localStorage data is corrupted/invalid JSON]
        if (typeof localStorage.removeItem === 'function') {
          localStorage.removeItem('dineease_user');
        }
      }
    }
  }

  // --- Authentication Operations ---
  
  /**
   * Logs in a user by sending credentials. Persists session in localStorage.
   */
  login(credentials: any): Observable<UserSession> {
    // [DEBUG BREAKPOINT: Break here to inspect login credentials sent from login page]
    return this.http.post<UserSession>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap({
        next: (user) => {
          // [DEBUG BREAKPOINT: Break here to verify successful user session payload returned]
          if (typeof localStorage !== 'undefined' && typeof localStorage.setItem === 'function') {
            localStorage.setItem('dineease_user', JSON.stringify(user));
          }
          this.currentUser.set(user);
        },
        error: (err) => {
          // [DEBUG BREAKPOINT: Break here if login failed (e.g. status 400 or bad credentials)]
        }
      })
    );
  }

  /**
   * Registers a new customer user.
   */
  register(userData: any): Observable<any> {
    // [DEBUG BREAKPOINT: Break here to inspect registration parameters]
    return this.http.post(`${this.apiUrl}/auth/register`, userData);
  }

  /**
   * Clears local user state and logs out.
   */
  logout() {
    if (typeof localStorage !== 'undefined' && typeof localStorage.removeItem === 'function') {
      localStorage.removeItem('dineease_user');
    }
    this.currentUser.set(null);
  }

  /**
   * Helper utility checking if the logged in user is an ADMIN.
   */
  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMIN';
  }

  /**
   * Helper utility checking if the user is authenticated.
   */
  isLoggedIn(): boolean {
    return this.currentUser() !== null;
  }

  // --- Dining Tables Operations ---

  /**
   * Fetches all dining tables.
   */
  getTables(): Observable<DiningTable[]> {
    return this.http.get<DiningTable[]>(`${this.apiUrl}/tables`);
  }

  /**
   * Creates a new dining table entity. (Admin only)
   */
  createTable(table: DiningTable): Observable<DiningTable> {
    // [DEBUG BREAKPOINT: Break here to check table parameters before sending]
    return this.http.post<DiningTable>(`${this.apiUrl}/tables`, table);
  }

  /**
   * Updates an existing dining table. (Admin only)
   */
  updateTable(id: number, table: DiningTable): Observable<DiningTable> {
    // [DEBUG BREAKPOINT: Break here to inspect table ID and properties to update]
    return this.http.put<DiningTable>(`${this.apiUrl}/tables/${id}`, table);
  }

  /**
   * Deletes a dining table. (Admin only)
   */
  deleteTable(id: number): Observable<void> {
    // [DEBUG BREAKPOINT: Break here to inspect table ID being deleted]
    return this.http.delete<void>(`${this.apiUrl}/tables/${id}`);
  }

  /**
   * Queries the backend for available tables suitable for a given guest count on a specific date/time.
   */
  searchAvailableTables(date: string, time: string, guestCount: number): Observable<DiningTable[]> {
    // [DEBUG BREAKPOINT: Break here to inspect query parameters: date, time, party size]
    return this.http.get<DiningTable[]>(
      `${this.apiUrl}/tables/search?date=${date}&time=${time}&guestCount=${guestCount}`
    );
  }

  // --- Reservations Operations ---

  /**
   * Fetches all table reservations. (Admin only)
   */
  getAllReservations(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.apiUrl}/reservations`);
  }

  /**
   * Fetches bookings associated with a specific customer email.
   */
  getMyBookings(email: string): Observable<Reservation[]> {
    // [DEBUG BREAKPOINT: Break here to inspect search email parameter]
    return this.http.get<Reservation[]>(`${this.apiUrl}/reservations/my-bookings?email=${email}`);
  }

  /**
   * books a table by sending customer and table reservation request payload.
   */
  bookTable(bookingRequest: any): Observable<Reservation> {
    // [DEBUG BREAKPOINT: Break here to inspect reservation form payload]
    return this.http.post<Reservation>(`${this.apiUrl}/reservations`, bookingRequest);
  }

  /**
   * Updates a reservation status (e.g., CONFIRMED, CANCELLED). (Admin only)
   */
  updateReservationStatus(id: number, status: string): Observable<Reservation> {
    // [DEBUG BREAKPOINT: Break here to inspect reservation ID and target status status]
    return this.http.put<Reservation>(`${this.apiUrl}/reservations/${id}/status?status=${status}`, {});
  }

  /**
   * Gathers dashboard statistics count. (Admin only)
   */
  getStats(): Observable<Stats> {
    return this.http.get<Stats>(`${this.apiUrl}/reservations/stats`);
  }
}

import { Injectable, signal, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface UserSession {
  username: string;
  email: string;
  role: string;
}

export interface DiningTable {
  id?: number;
  tableNumber: number;
  capacity: number;
  status: string;
}

export interface Reservation {
  id?: number;
  diningTable: DiningTable;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  reservationDate: string;
  reservationTime: string;
  numberOfGuests: number;
  status: string;
  specialRequests?: string;
}

export interface Stats {
  totalTables: number;
  activeReservationsToday: number;
  pendingReservationsCount: number;
  totalReservations: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = 'http://localhost:8080/api';
  
  // Angular Signal to track current user state
  public currentUser = signal<UserSession | null>(null);

  constructor(private http: HttpClient) {
    // Load session on startup
    if (typeof localStorage !== 'undefined' && typeof localStorage.getItem === 'function') {
      try {
        const savedUser = localStorage.getItem('dineease_user');
        if (savedUser) {
          this.currentUser.set(JSON.parse(savedUser));
        }
      } catch (e) {
        if (typeof localStorage.removeItem === 'function') {
          localStorage.removeItem('dineease_user');
        }
      }
    }
  }

  // --- Authentication ---
  login(credentials: any): Observable<UserSession> {
    return this.http.post<UserSession>(`${this.apiUrl}/auth/login`, credentials).pipe(
      tap(user => {
        if (typeof localStorage !== 'undefined' && typeof localStorage.setItem === 'function') {
          localStorage.setItem('dineease_user', JSON.stringify(user));
        }
        this.currentUser.set(user);
      })
    );
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, userData);
  }

  logout() {
    if (typeof localStorage !== 'undefined' && typeof localStorage.removeItem === 'function') {
      localStorage.removeItem('dineease_user');
    }
    this.currentUser.set(null);
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMIN';
  }

  isLoggedIn(): boolean {
    return this.currentUser() !== null;
  }

  // --- Dining Tables ---
  getTables(): Observable<DiningTable[]> {
    return this.http.get<DiningTable[]>(`${this.apiUrl}/tables`);
  }

  createTable(table: DiningTable): Observable<DiningTable> {
    return this.http.post<DiningTable>(`${this.apiUrl}/tables`, table);
  }

  updateTable(id: number, table: DiningTable): Observable<DiningTable> {
    return this.http.put<DiningTable>(`${this.apiUrl}/tables/${id}`, table);
  }

  deleteTable(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/tables/${id}`);
  }

  searchAvailableTables(date: string, time: string, guestCount: number): Observable<DiningTable[]> {
    return this.http.get<DiningTable[]>(
      `${this.apiUrl}/tables/search?date=${date}&time=${time}&guestCount=${guestCount}`
    );
  }

  // --- Reservations ---
  getAllReservations(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.apiUrl}/reservations`);
  }

  getMyBookings(email: string): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.apiUrl}/reservations/my-bookings?email=${email}`);
  }

  bookTable(bookingRequest: any): Observable<Reservation> {
    return this.http.post<Reservation>(`${this.apiUrl}/reservations`, bookingRequest);
  }

  updateReservationStatus(id: number, status: string): Observable<Reservation> {
    return this.http.put<Reservation>(`${this.apiUrl}/reservations/${id}/status?status=${status}`, {});
  }

  getStats(): Observable<Stats> {
    return this.http.get<Stats>(`${this.apiUrl}/reservations/stats`);
  }
}

import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiService, Reservation } from '../../services/api.service';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-bookings.html'
})
export class MyBookings {
  bookings = signal<Reservation[]>([]);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  constructor(protected api: ApiService, private router: Router) {
    if (!this.api.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.fetchBookings();
  }

  fetchBookings() {
    const user = this.api.currentUser();
    if (!user) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.api.getMyBookings(user.email).subscribe({
      next: (res) => {
        this.bookings.set(res);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load reservation history. Verify your connection.');
      }
    });
  }

  cancelBooking(id: number) {
    if (!confirm('Are you sure you want to cancel this reservation request?')) {
      return;
    }

    this.api.updateReservationStatus(id, 'CANCELLED').subscribe({
      next: () => {
        // Refresh local bookings list
        this.fetchBookings();
      },
      error: (err) => {
        alert(err.error?.message || 'Failed to cancel reservation.');
      }
    });
  }
}

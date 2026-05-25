import { Component, signal, effect } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ApiService, DiningTable } from '../../services/api.service';

@Component({
  selector: 'app-book-table',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './book-table.html'
})
export class BookTable {
  // Search parameters
  searchDate = '';
  searchTime = '18:00:00';
  guestCount = 2;

  // Search status
  tables = signal<DiningTable[]>([]);
  searched = signal(false);
  isLoadingTables = signal(false);
  errorMessage = signal<string | null>(null);

  // Selection
  selectedTable = signal<DiningTable | null>(null);

  // Booking Form parameters
  customerName = '';
  customerEmail = '';
  customerPhone = '';
  specialRequests = '';
  isSubmittingBooking = signal(false);
  bookingSuccess = signal(false);

  // Time slots for selection
  timeSlots = [
    { label: '11:00 AM', value: '11:00:00' },
    { label: '12:00 PM', value: '12:00:00' },
    { label: '1:00 PM', value: '13:00:00' },
    { label: '2:00 PM', value: '14:00:00' },
    { label: '3:00 PM', value: '15:00:00' },
    { label: '4:00 PM', value: '16:00:00' },
    { label: '5:00 PM', value: '17:00:00' },
    { label: '6:00 PM', value: '18:00:00' },
    { label: '7:00 PM', value: '19:00:00' },
    { label: '8:00 PM', value: '20:00:00' },
    { label: '9:00 PM', value: '21:00:00' },
    { label: '10:00 PM', value: '22:00:00' }
  ];

  constructor(protected api: ApiService, private router: Router) {
    // Set default date to today
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    this.searchDate = `${yyyy}-${mm}-${dd}`;

    // Prefill form if user is logged in
    const currentUser = this.api.currentUser();
    if (currentUser) {
      this.customerName = currentUser.username;
      this.customerEmail = currentUser.email;
    }

    // React to user session updates
    effect(() => {
      const user = this.api.currentUser();
      if (user) {
        if (!this.customerName) this.customerName = user.username;
        if (!this.customerEmail) this.customerEmail = user.email;
      }
    });
  }

  onSearch() {
    if (!this.searchDate || !this.searchTime) {
      this.errorMessage.set('Please fill out date and time.');
      return;
    }

    this.isLoadingTables.set(true);
    this.errorMessage.set(null);
    this.searched.set(false);
    this.selectedTable.set(null);

    this.api.searchAvailableTables(this.searchDate, this.searchTime, this.guestCount).subscribe({
      next: (res) => {
        this.tables.set(res);
        this.searched.set(true);
        this.isLoadingTables.set(false);
        if (res.length === 0) {
          this.errorMessage.set('No tables available matching your criteria. Try changing the time slot or party size.');
        }
      },
      error: (err) => {
        this.isLoadingTables.set(false);
        this.errorMessage.set('Failed to search tables. Verify your connection.');
      }
    });
  }

  selectTable(table: DiningTable) {
    this.selectedTable.set(table);
  }

  filteredTables(): DiningTable[] {
    return this.tables();
  }

  onSubmitBooking() {
    const table = this.selectedTable();
    if (!table) {
      this.errorMessage.set('Please select a table to complete booking.');
      return;
    }

    if (!this.customerName || !this.customerEmail || !this.customerPhone) {
      this.errorMessage.set('Please fill out all contact fields.');
      return;
    }

    this.isSubmittingBooking.set(true);
    this.errorMessage.set(null);

    const bookingRequest = {
      diningTableId: table.id,
      customerName: this.customerName,
      customerEmail: this.customerEmail,
      customerPhone: this.customerPhone,
      reservationDate: this.searchDate,
      reservationTime: this.searchTime,
      numberOfGuests: this.guestCount,
      specialRequests: this.specialRequests
    };

    this.api.bookTable(bookingRequest).subscribe({
      next: () => {
        this.isSubmittingBooking.set(false);
        this.bookingSuccess.set(true);
        this.tables.set([]);
        this.searched.set(false);
        this.selectedTable.set(null);
        
        // Wait and redirect
        setTimeout(() => {
          if (this.api.isLoggedIn()) {
            this.router.navigate(['/my-bookings']);
          } else {
            this.router.navigate(['/']);
          }
        }, 3000);
      },
      error: (err) => {
        this.isSubmittingBooking.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to submit booking. Table might have been booked in the meantime.');
      }
    });
  }
}

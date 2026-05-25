import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, Reservation, DiningTable, Stats } from '../../services/api.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-dashboard.html'
})
export class AdminDashboard {
  // Lists
  reservations = signal<Reservation[]>([]);
  tables = signal<DiningTable[]>([]);
  
  // Stats
  stats = signal<Stats>({
    totalTables: 0,
    activeReservationsToday: 0,
    pendingReservationsCount: 0,
    totalReservations: 0
  });

  // Table form state
  newTableNumber: number | null = null;
  newCapacity = 4;

  // Edit Table modal/form state
  editingTableId: number | null = null;
  editTableNumber: number | null = null;
  editCapacity = 4;
  editStatus = 'AVAILABLE';

  // Toggle view
  activeTab = 'reservations'; // 'reservations' or 'tables'

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  constructor(private api: ApiService, private router: Router) {
    if (!this.api.isLoggedIn() || !this.api.isAdmin()) {
      this.router.navigate(['/login']);
      return;
    }
    this.loadAllData();
  }

  loadAllData() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    // Call stats, reservations and tables concurrently
    this.loadStats();
    this.loadReservations();
    this.loadTables();
  }

  loadStats() {
    this.api.getStats().subscribe({
      next: (res) => this.stats.set(res),
      error: () => this.errorMessage.set('Failed to load system stats.')
    });
  }

  loadReservations() {
    this.api.getAllReservations().subscribe({
      next: (res) => {
        // Sort reservations by ID descending (newest first)
        this.reservations.set(res.sort((a, b) => (b.id || 0) - (a.id || 0)));
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('Failed to load reservations list.');
      }
    });
  }

  loadTables() {
    this.api.getTables().subscribe({
      next: (res) => {
        this.tables.set(res.sort((a, b) => a.tableNumber - b.tableNumber));
      },
      error: () => this.errorMessage.set('Failed to load tables inventory.')
    });
  }

  // --- Reservation Management ---
  approveReservation(id: number) {
    this.api.updateReservationStatus(id, 'CONFIRMED').subscribe({
      next: () => {
        this.successMessage.set(`Approved Reservation #DE-${id}`);
        this.clearMessagesDelayed();
        this.loadAllData();
      },
      error: (err) => alert(err.error?.message || 'Failed to approve booking.')
    });
  }

  cancelReservation(id: number) {
    if (!confirm(`Are you sure you want to cancel Reservation #DE-${id}?`)) {
      return;
    }

    this.api.updateReservationStatus(id, 'CANCELLED').subscribe({
      next: () => {
        this.successMessage.set(`Cancelled Reservation #DE-${id}`);
        this.clearMessagesDelayed();
        this.loadAllData();
      },
      error: (err) => alert(err.error?.message || 'Failed to cancel booking.')
    });
  }

  // --- Table Management ---
  onAddTable() {
    if (!this.newTableNumber || this.newTableNumber <= 0) {
      alert('Please enter a valid table number.');
      return;
    }

    const payload: DiningTable = {
      tableNumber: this.newTableNumber,
      capacity: this.newCapacity,
      status: 'AVAILABLE'
    };

    this.api.createTable(payload).subscribe({
      next: () => {
        this.successMessage.set(`Table ${payload.tableNumber} added successfully.`);
        this.clearMessagesDelayed();
        this.newTableNumber = null;
        this.loadAllData();
      },
      error: (err) => alert(err.error?.message || 'Failed to create table. Table number might already exist.')
    });
  }

  startEditTable(table: DiningTable) {
    this.editingTableId = table.id!;
    this.editTableNumber = table.tableNumber;
    this.editCapacity = table.capacity;
    this.editStatus = table.status;
  }

  cancelEditTable() {
    this.editingTableId = null;
  }

  onUpdateTable() {
    if (!this.editingTableId) return;

    const payload: DiningTable = {
      id: this.editingTableId,
      tableNumber: this.editTableNumber!,
      capacity: this.editCapacity,
      status: this.editStatus
    };

    this.api.updateTable(this.editingTableId, payload).subscribe({
      next: () => {
        this.successMessage.set(`Table ${payload.tableNumber} updated successfully.`);
        this.clearMessagesDelayed();
        this.editingTableId = null;
        this.loadAllData();
      },
      error: (err) => alert(err.error?.message || 'Failed to update table. Verify settings.')
    });
  }

  onDeleteTable(id: number) {
    if (!confirm('Are you sure you want to delete this table? Any active reservations referencing it may fail.')) {
      return;
    }

    this.api.deleteTable(id).subscribe({
      next: () => {
        this.successMessage.set('Table deleted successfully.');
        this.clearMessagesDelayed();
        this.loadAllData();
      },
      error: (err) => alert(err.error?.message || 'Failed to delete table.')
    });
  }

  private clearMessagesDelayed() {
    setTimeout(() => {
      this.successMessage.set(null);
    }, 4000);
  }
}

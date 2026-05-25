import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html'
})
export class Login {
  username = '';
  password = '';
  errorMessage = signal<string | null>(null);
  isLoading = signal(false);

  constructor(private api: ApiService, private router: Router) {
    // If already logged in, redirect
    if (this.api.isLoggedIn()) {
      this.redirectUser();
    }
  }

  onSubmit() {
    if (!this.username || !this.password) {
      this.errorMessage.set('Please enter both username and password.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.api.login({ username: this.username, password: this.password }).subscribe({
      next: (user) => {
        this.isLoading.set(false);
        this.redirectUser();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Invalid username or password.');
      }
    });
  }

  private redirectUser() {
    if (this.api.isAdmin()) {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigate(['/my-bookings']);
    }
  }
}

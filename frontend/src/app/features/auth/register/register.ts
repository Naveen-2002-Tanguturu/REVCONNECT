
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, RegisterRequest } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrls: ['./register.scss']
})
export class Register {

  details: RegisterRequest = {
    name: '',
    username: '',
    email: '',
    password: '',
    userType: 'PERSONAL',
    category: '',
    address: '',
    contactEmail: '',
    contactPhone: '',
    logoUrl: '',
    coverImageUrl: ''
  };

  isLoading = false;
  errorMessage = '';
  passwordStrength = 0;
  showPassword = false;
  readonly emailPattern = '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$';

  constructor(private authService: AuthService, private router: Router) { }

  onTypeChange() {
    this.details.category = '';
    this.details.address = '';
    this.details.contactEmail = '';
    this.details.contactPhone = '';
    this.details.logoUrl = '';
    this.details.coverImageUrl = '';
  }

  checkPasswordStrength() {
    const pbox = this.details.password;
    this.passwordStrength = 0;
    if (!pbox) return;

    if (pbox.length >= 6) this.passwordStrength += 25;
    if (/[A-Z]/.test(pbox)) this.passwordStrength += 25;
    if (/[0-9]/.test(pbox)) this.passwordStrength += 25;
    if (/[^A-Za-z0-9]/.test(pbox)) this.passwordStrength += 25;
  }

  onSubmit(registerForm: NgForm) {
    if (registerForm.invalid) {
      registerForm.control.markAllAsTouched();
      return;
    }

    this.details.email = this.details.email.trim();
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.register(this.details).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.success && response.data) {
          alert('Registration successful! Redirecting to login.');
          this.router.navigate(['/login']);
        } else {
          this.errorMessage = response.message || 'Registration failed.';
        }
      },
      error: (error) => {
        this.isLoading = false;
        console.error('Registration error:', error);
        this.errorMessage =
          error.error?.message ||
          `Registration failed (Status: ${error.status}). Please try again.`;
        alert('Error: ' + this.errorMessage);
      }
    });
  }
}


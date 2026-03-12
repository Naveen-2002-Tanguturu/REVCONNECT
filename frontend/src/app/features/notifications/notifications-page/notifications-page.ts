import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Navbar } from '../../../core/components/navbar/navbar';
import { Sidebar } from '../../../core/components/sidebar/sidebar';
import { NotificationService, NotificationResponse } from '../../../core/services/notification.service';
import { SearchService } from '../../../core/services/search.service';
import { UserService, UserResponse } from '../../../core/services/user.service';
import { ConnectionService } from '../../../core/services/connection.service';
import { RouterModule, Router } from '@angular/router';
import { BottomNav } from '../../../core/components/bottom-nav/bottom-nav';

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [CommonModule, Navbar, Sidebar, RouterModule, BottomNav],
  providers: [DatePipe],
  templateUrl: './notifications-page.html',
  styleUrls: ['./notifications-page.css']
})
export class NotificationsPage implements OnInit {
  notifications: any[] = [];
  isLoading = false;
  page = 0;
  totalPages = 1;

  constructor(
    private notificationService: NotificationService,
    private searchService: SearchService,
    private userService: UserService,
    private connectionService: ConnectionService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.loadNotifications();
  }

  viewProfile(userId: number) {
    this.router.navigate(['/profile', userId]);
  }

  loadNotifications() {
    this.isLoading = true;
    this.notificationService.getNotifications(this.page, 20).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.notifications = res.data.content.map((n: any) => ({
            ...n,
            isRead: n.isRead === true || n.read === true
          }));
          this.totalPages = res.data.totalPages;
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  markAsRead(notification: any) {
    if (notification.isRead) return;

    // Optimistic UI update
    notification.isRead = true;
    this.notifications = [...this.notifications];
    this.cdr.detectChanges();

    this.notificationService.markAsRead(notification.id).subscribe({
      error: () => {
        // Revert on error
        notification.isRead = false;
        this.notifications = [...this.notifications];
        this.cdr.detectChanges();
      }
    });
  }

  navigateNotification(notification: any) {
    // Mark as read first
    if (!notification.isRead) {
      notification.isRead = true;
      this.notifications = [...this.notifications];
      this.cdr.detectChanges();
      this.notificationService.markAsRead(notification.id).subscribe();
    }

    // Navigate based on type
    const type: string = notification.type || '';
    switch (type) {
      case 'LIKE':
      case 'COMMENT':
      case 'SHARE':
        if (notification.postId) {
          this.router.navigate(['/post', notification.postId]);
        } else {
          this.router.navigate(['/feed']);
        }
        break;
      case 'FOLLOW':
      case 'NEW_FOLLOWER':
      case 'CONNECTION_REQUEST':
      case 'CONNECTION_ACCEPTED':
        if (notification.senderId) {
          this.router.navigate(['/profile', notification.senderId]);
        } else if (notification.actorId) {
          this.router.navigate(['/profile', notification.actorId]);
        }
        break;
      default:
        // No specific route, just mark read
        break;
    }
  }

  markAllAsRead() {
    this.notifications = this.notifications.map(n => ({ ...n, isRead: true }));
    this.cdr.detectChanges();

    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notificationService.refreshUnreadCount();
      }
    });
  }

  deleteNotification(notification: any, event: Event) {
    event.stopPropagation();

    this.notifications = this.notifications.filter(n => n.id !== notification.id);
    this.cdr.detectChanges();

    this.notificationService.deleteNotification(notification.id).subscribe({
      error: () => {
        this.loadNotifications();
      }
    });
  }

  getIconForType(type: string): string {
    switch (type) {
      case 'LIKE': return 'fa-solid fa-heart text-danger';
      case 'COMMENT': return 'fa-solid fa-comment text-primary';
      case 'FOLLOW':
      case 'NEW_FOLLOWER': return 'fa-solid fa-user-plus text-success';
      case 'SHARE': return 'fa-solid fa-share text-warning';
      case 'CONNECTION_REQUEST': return 'fa-solid fa-user-clock text-warning';
      case 'CONNECTION_ACCEPTED': return 'fa-solid fa-check-circle text-success';
      case 'BRAND_UPDATE': return 'fa-solid fa-bullhorn text-info';
      default: return 'fa-solid fa-bell text-info';
    }
  }

  getRelativeTime(dateString: string): string {
    if (!dateString) return '';
    const date = new Date((dateString || '').endsWith('Z') ? dateString : dateString + 'Z');
    const now = new Date();
    const seconds = Math.max(0, Math.floor((now.getTime() - date.getTime()) / 1000));

    let interval = seconds / 31536000;
    if (interval > 1) return Math.floor(interval) + 'y';
    interval = seconds / 2592000;
    if (interval > 1) return Math.floor(interval) + 'mo';
    interval = seconds / 86400;
    if (interval > 1) return Math.floor(interval) + 'd';
    interval = seconds / 3600;
    if (interval > 1) return Math.floor(interval) + 'h';
    interval = seconds / 60;
    if (interval > 1) return Math.floor(interval) + 'm';
    return Math.floor(seconds) + 's';
  }
}

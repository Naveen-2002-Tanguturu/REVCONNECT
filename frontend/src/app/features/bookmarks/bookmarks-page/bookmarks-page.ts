import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Navbar } from '../../../core/components/navbar/navbar';
import { Sidebar } from '../../../core/components/sidebar/sidebar';
import { BookmarkService, BookmarkResponse } from '../../../core/services/bookmark.service';
import { SearchService } from '../../../core/services/search.service';
import { UserService, UserResponse } from '../../../core/services/user.service';
import { ConnectionService } from '../../../core/services/connection.service';
import { HashtagTextComponent } from '../../../shared/components/hashtag-text/hashtag-text.component';

@Component({
    selector: 'app-bookmarks-page',
    standalone: true,
    imports: [CommonModule, RouterModule, Navbar, Sidebar, HashtagTextComponent],
    templateUrl: './bookmarks-page.html',
    styleUrls: ['./bookmarks-page.css']
})
export class BookmarksPage implements OnInit {
    bookmarks: BookmarkResponse[] = [];
    isLoading = false;

    constructor(
        private bookmarkService: BookmarkService,
        private searchService: SearchService,
        private userService: UserService,
        private connectionService: ConnectionService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) { }

    ngOnInit() {
        this.loadBookmarks();
    }

    loadBookmarks() {
        this.isLoading = true;
        this.cdr.markForCheck();

        this.bookmarkService.getBookmarks(0, 20).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.bookmarks = res.data.content;
                }
                this.isLoading = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.isLoading = false;
                this.cdr.markForCheck();
            }
        });
    }

    // Use post.id (from embedded PostResponse) - there is no standalone postId on BookmarkResponse
    removeBookmark(postId: number) {
        this.bookmarkService.removeBookmark(postId).subscribe({
            next: () => {
                this.bookmarks = this.bookmarks.filter(b => b.post?.id !== postId);
                this.cdr.markForCheck();
            }
        });
    }

    viewProfile(userId: number) {
        this.router.navigate(['/profile', userId]);
    }

    viewFullPost(postId: number) {
        // Since there is no standalone post page yet, we navigate to the author's profile
        // where the user can find the post in their feed.
        const bookmark = this.bookmarks.find(b => b.post.id === postId);
        if (bookmark?.post.authorId) {
            this.router.navigate(['/profile', bookmark.post.authorId]);
        }
    }

    getRelativeTime(dateString: string | undefined): string {
        if (!dateString) return '';
        const date = new Date(dateString);
        const now = new Date();
        const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);
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
        return 'just now';
    }
}

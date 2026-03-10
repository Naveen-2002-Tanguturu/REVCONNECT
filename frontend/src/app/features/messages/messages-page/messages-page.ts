import { Component, OnInit, OnDestroy, ChangeDetectorRef, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Navbar } from '../../../core/components/navbar/navbar';
import { Sidebar } from '../../../core/components/sidebar/sidebar';
import { MessageService, ConversationPartner, MessageItem } from '../../../core/services/message.service';
import { UserService } from '../../../core/services/user.service';

/**
 * HOW MESSAGES WORK (connected to backend MessageController):
 *
 * Backend uses the OTHER USER's ID as the "conversationId":
 *   - GET  /api/messages/conversations          → list of users you've chatted with
 *   - POST /api/messages/conversations?recipientId=X  → start/open a conversation
 *   - GET  /api/messages/conversations/{userId}  → fetch messages with that user
 *   - POST /api/messages/conversations/{userId}?content=X  → send a message to that user
 *   - POST /api/messages/conversations/{userId}/read  → mark all messages as read
 *   - GET  /api/messages/unread/count            → total unread message count
 *
 * So "conversationId" = the OTHER user's userId throughout.
 */
@Component({
    selector: 'app-messages-page',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule, Navbar, Sidebar],
    templateUrl: './messages-page.html',
    styleUrls: ['./messages-page.scss']
})
export class MessagesPage implements OnInit, OnDestroy {
    @ViewChild('messagesEnd') messagesEnd?: ElementRef;

    conversations: ConversationPartner[] = [];
    selectedConversation: ConversationPartner | null = null;
    messages: MessageItem[] = [];
    newMessage = '';

    isLoadingConversations = false;
    isLoadingMessages = false;
    isSending = false;

    currentUserId: number = 0;

    constructor(
        private messageService: MessageService,
        private userService: UserService,
        private route: ActivatedRoute,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) { }

    ngOnInit() {
        this.loadCurrentUser();

        // If navigated from profile page with a userId, open that conversation
        this.route.queryParams.subscribe(params => {
            const userId = params['userId'];
            if (userId) {
                const partnerId = +userId;
                // First create/open the conversation with this user
                this.messageService.createConversation(partnerId).subscribe({
                    next: (res) => {
                        if (res.success && res.data) {
                            // Automatically select the partner returned from the API
                            this.selectConversation(res.data);
                        }
                        // Still load all conversations in the background for sidebar list
                        this.loadConversations();
                    },
                    error: () => {
                        // Fallback if needed
                        this.loadConversationsAndSelect(partnerId);
                    }
                });
            } else {
                // If no specific userId is passed, just load the list of conversations normally
                this.loadConversations();
            }
        });
    }

    ngOnDestroy() { }

    loadCurrentUser() {
        this.userService.getMyProfile().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.currentUserId = res.data.id;
                    this.cdr.markForCheck();
                }
            }
        });
    }

    loadConversations() {
        this.isLoadingConversations = true;
        this.cdr.markForCheck();

        // GET /api/messages/conversations
        this.messageService.getConversations().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.conversations = res.data;
                }
                this.isLoadingConversations = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.isLoadingConversations = false;
                this.cdr.markForCheck();
            }
        });
    }

    loadConversationsAndSelect(partnerId: number) {
        this.messageService.getConversations().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.conversations = res.data;
                    // Auto-select the conversation with this partner
                    const partner = this.conversations.find(c => Number(c.userId) === partnerId);
                    if (partner) {
                        this.selectConversation(partner);
                    } else {
                        // Partner not in list yet - load their profile and add
                        this.userService.getUserById(partnerId).subscribe({
                            next: (userRes) => {
                                if (userRes.success && userRes.data) {
                                    const newPartner: ConversationPartner = {
                                        userId: userRes.data.id,
                                        username: userRes.data.username,
                                        name: userRes.data.name,
                                        profilePicture: userRes.data.profilePicture || ''
                                    };
                                    // Make sure we don't accidentally add duplicates if it somehow resolved in the meantime
                                    if (!this.conversations.some(c => Number(c.userId) === partnerId)) {
                                        this.conversations.unshift(newPartner);
                                    }
                                    this.selectConversation(newPartner);
                                    this.cdr.markForCheck();
                                }
                            }
                        });
                    }
                }
                this.cdr.markForCheck();
            }
        });
    }

    selectConversation(partner: ConversationPartner) {
        this.selectedConversation = partner;
        this.messages = [];
        this.loadMessages(partner.userId);
        // Ensure the partner is in the conversations list (for sidebar highlighting)
        if (!this.conversations.some(c => Number(c.userId) === Number(partner.userId))) {
            this.conversations.unshift(partner);
        }
        // POST /api/messages/conversations/{userId}/read
        this.messageService.markConversationAsRead(partner.userId).subscribe();
        this.cdr.markForCheck();
    }

    loadMessages(conversationId: number) {
        this.isLoadingMessages = true;
        this.cdr.markForCheck();

        // GET /api/messages/conversations/{userId}?page=0&size=50
        this.messageService.getMessages(conversationId, 0, 50).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    // Backend returns newest first — reverse for chronological display
                    this.messages = [...res.data].reverse();
                }
                this.isLoadingMessages = false;
                this.cdr.markForCheck();
                setTimeout(() => this.scrollToBottom(), 100);
            },
            error: () => {
                this.isLoadingMessages = false;
                this.cdr.markForCheck();
            }
        });
    }

    sendMessage() {
        const content = this.newMessage.trim();
        if (!content || !this.selectedConversation || this.isSending) return;

        this.isSending = true;
        const conversationId = this.selectedConversation.userId;
        const tempMessage = content;
        this.newMessage = '';
        this.cdr.markForCheck();

        // POST /api/messages/conversations/{userId}?content=...
        this.messageService.sendMessage(conversationId, tempMessage).subscribe({
            next: (res) => {
                if (res.success) {
                    // Reload messages to show new message in thread
                    this.loadMessages(conversationId);
                }
                this.isSending = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.newMessage = tempMessage; // restore on error
                this.isSending = false;
                this.cdr.markForCheck();
            }
        });
    }

    isMine(message: MessageItem): boolean {
        return message.senderId === this.currentUserId;
    }

    getAvatarUrl(partner: ConversationPartner): string {
        return partner.profilePicture ||
            `https://ui-avatars.com/api/?name=${encodeURIComponent(partner.name || partner.username)}&background=random`;
    }

    getRelativeTime(dateString: string): string {
        if (!dateString) return '';
        const date = new Date(dateString);
        const now = new Date();
        const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);
        let interval = seconds / 86400;
        if (interval > 1) return Math.floor(interval) + 'd ago';
        interval = seconds / 3600;
        if (interval > 1) return Math.floor(interval) + 'h ago';
        interval = seconds / 60;
        if (interval > 1) return Math.floor(interval) + 'm ago';
        return 'just now';
    }

    scrollToBottom() {
        try {
            if (this.messagesEnd) {
                this.messagesEnd.nativeElement.scrollIntoView({ behavior: 'smooth' });
            }
        } catch { }
    }
}

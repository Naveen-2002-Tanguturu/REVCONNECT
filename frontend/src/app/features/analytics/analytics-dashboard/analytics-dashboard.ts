import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Navbar } from '../../../core/components/navbar/navbar';
import { Sidebar } from '../../../core/components/sidebar/sidebar';
import { AnalyticsService, AnalyticsOverview, PostPerformance, FollowerGrowth } from '../../../core/services/analytics.service';
import { UserService, UserResponse } from '../../../core/services/user.service';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [CommonModule, Navbar, Sidebar, RouterModule],
  templateUrl: './analytics-dashboard.html',
  styleUrl: './analytics-dashboard.scss',
})
export class AnalyticsDashboard implements OnInit {
  currentUser: UserResponse | null = null;
  overview: AnalyticsOverview | null = null;
  topPosts: any[] = [];
  postPerformance: PostPerformance[] = [];
  followerGrowth: FollowerGrowth[] = [];
  engagement: any = null;


  // News items for discovery
  allNews = [
    {
      category: 'Sports',
      title: 'Championship Finals Set',
      description: 'Anticipation builds globally as the underdog team secures their spot in the ultimate showdown.',
      imageUrl: 'https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?q=80&w=2605&auto=format&fit=crop'
    },
    {
      category: 'Cinema',
      title: 'Blockbuster Success',
      description: 'The highly anticipated sequel shatters box office records on its opening weekend worldwide.',
      imageUrl: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?q=80&w=2670&auto=format&fit=crop'
    },
    {
      category: 'Finance',
      title: 'Market Updates',
      description: 'Global markets see an unexpected surge following new international trade agreements.',
      imageUrl: 'https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?q=80&w=2670&auto=format&fit=crop'
    },
    {
      category: 'Technology',
      title: 'AI Breaktrough',
      description: 'Researchers unveil a new AI model capable of reasoning through complex quantum physics problems.',
      imageUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=2672&auto=format&fit=crop'
    },
    {
      category: 'Gaming',
      title: 'E3 Highlights',
      description: 'Next-generation consoles and highly anticipated titles steal the show at this year\'s convention.',
      imageUrl: 'https://images.unsplash.com/photo-1511512578047-dfb367046420?q=80&w=2671&auto=format&fit=crop'
    },
    {
      category: 'Music',
      title: 'Summer Festivals',
      description: 'Millions gather across the globe to celebrate live music\'s massive resurgence this season.',
      imageUrl: 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?q=80&w=2670&auto=format&fit=crop'
    },
    {
      category: 'Politics',
      title: 'Global Summit',
      description: 'World leaders convene to discuss urgent climate action and international trade policies.',
      imageUrl: 'https://images.unsplash.com/photo-1529107386315-e1a2ed48a620?q=80&w=2670&auto=format&fit=crop'
    }
  ];
  randomNews: any[] = [];

  isLoading = true;
  activePeriod = 30; // 7, 30, 90 days

  constructor(
    private analyticsService: AnalyticsService,
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.userService.getMyProfile().subscribe({
      next: (res) => {
        if (res.success) this.currentUser = res.data;
        this.randomizeNews();
        this.loadData();
      }
    });
  }

  randomizeNews(): void {
    // Shuffle the array and pick the first 3
    const shuffled = [...this.allNews].sort(() => 0.5 - Math.random());
    this.randomNews = shuffled.slice(0, 3);
  }

  loadData(): void {
    this.isLoading = true;
    this.cdr.markForCheck();

    forkJoin({
      overview: this.analyticsService.getOverview(),
      topPosts: this.analyticsService.getTopPosts(5),
      performance: this.analyticsService.getPostPerformance(this.activePeriod),
      growth: this.analyticsService.getFollowerGrowth(this.activePeriod),
      engagement: this.analyticsService.getEngagement(this.activePeriod),
      demographics: this.analyticsService.getAudienceDemographics()
    }).subscribe({
      next: (results) => {
        if (results.overview.success) this.overview = results.overview.data!;
        if (results.topPosts.success) this.topPosts = results.topPosts.data!;
        if (results.performance.success) this.postPerformance = results.performance.data!;
        if (results.growth.success) this.followerGrowth = results.growth.data!;
        if (results.engagement.success) this.engagement = results.engagement.data;

        // Store demographics for the view
        this.audienceDemographics = results.demographics.data;

        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Error loading analytics:', err);
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  audienceDemographics: any = null;

  setPeriod(days: number): void {
    this.activePeriod = days;
    this.loadData();
  }

  // Helper for mock visualization (bar height calculation)
  getBarHeight(value: number, max: number): string {
    if (max === 0) return '0%';
    return (value / max * 100) + '%';
  }

  getMaxGrowthValue(): number {
    if (!this.followerGrowth || this.followerGrowth.length === 0) return 0;
    return Math.max(...this.followerGrowth.map(g => g.followers));
  }
}

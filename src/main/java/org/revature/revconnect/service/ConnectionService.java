package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.ConnectionResponse;
import org.revature.revconnect.dto.response.ConnectionStatsResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.ConnectionMapper;
import org.revature.revconnect.model.Connection;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final ConnectionMapper connectionMapper;

    @Transactional
    public void followUser(Long userId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to follow user {}", currentUser.getUsername(), userId);

        if (currentUser.getId().equals(userId)) {
            log.warn("User {} attempted to follow themselves", currentUser.getUsername());
            throw new BadRequestException("You cannot follow yourself");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (connectionRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), userId)) {
            log.warn("User {} already follows/requested user {}", currentUser.getUsername(), userId);
            throw new BadRequestException("You already follow or have a pending request for this user");
        }

        Connection connection = Connection.builder()
                .follower(currentUser)
                .following(targetUser)
                .status(ConnectionStatus.ACCEPTED) // Auto-accept for now
                .build();

        connectionRepository.save(connection);

        notificationService.notifyFollow(targetUser, currentUser);

        log.info("User {} now follows user {}", currentUser.getUsername(), targetUser.getUsername());
    }

    @Transactional
    public void unfollowUser(Long userId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to unfollow user {}", currentUser.getUsername(), userId);

        Connection connection = connectionRepository.findByFollowerIdAndFollowingId(currentUser.getId(), userId)
                .orElseThrow(() -> new BadRequestException("You are not following this user"));

        connectionRepository.delete(connection);
        log.info("User {} unfollowed user {}", currentUser.getUsername(), userId);
    }

    public PagedResponse<ConnectionResponse> getFollowers(Long userId, int page, int size) {
        log.info("Fetching followers for user ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        Page<Connection> followers = connectionRepository.findFollowersByUserId(
                userId, ConnectionStatus.ACCEPTED, PageRequest.of(page, size));

        log.info("Found {} followers for user {}", followers.getTotalElements(), userId);
        return PagedResponse.fromEntityPage(followers, connectionMapper::fromFollower);
    }

    public PagedResponse<ConnectionResponse> getFollowing(Long userId, int page, int size) {
        log.info("Fetching following for user ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        Page<Connection> following = connectionRepository.findFollowingByUserId(
                userId, ConnectionStatus.ACCEPTED, PageRequest.of(page, size));

        log.info("Found {} following for user {}", following.getTotalElements(), userId);
        return PagedResponse.fromEntityPage(following, connectionMapper::fromFollowing);
    }

    public PagedResponse<ConnectionResponse> getPendingRequests(int page, int size) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching pending requests for user: {}", currentUser.getUsername());

        Page<Connection> pending = connectionRepository.findPendingRequestsByUserId(
                currentUser.getId(), PageRequest.of(page, size));

        log.info("Found {} pending requests", pending.getTotalElements());
        return PagedResponse.fromEntityPage(pending, connectionMapper::fromFollower);
    }

    @Transactional
    public void acceptRequest(Long connectionId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} accepting connection request {}", currentUser.getUsername(), connectionId);

        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection", "id", connectionId));

        if (!connection.getFollowing().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only accept requests sent to you");
        }

        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new BadRequestException("This request is not pending");
        }

        connection.setStatus(ConnectionStatus.ACCEPTED);
        connectionRepository.save(connection);

        notificationService.notifyConnectionAccepted(connection.getFollower(), currentUser);

        log.info("Connection request {} accepted", connectionId);
    }

    @Transactional
    public void rejectRequest(Long connectionId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} rejecting connection request {}", currentUser.getUsername(), connectionId);

        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection", "id", connectionId));

        if (!connection.getFollowing().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only reject requests sent to you");
        }

        connectionRepository.delete(connection);
        log.info("Connection request {} rejected", connectionId);
    }

    public ConnectionStatsResponse getConnectionStats(Long userId) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching connection stats for user ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        long followersCount = connectionRepository.countByFollowingIdAndStatus(userId, ConnectionStatus.ACCEPTED);
        long followingCount = connectionRepository.countByFollowerIdAndStatus(userId, ConnectionStatus.ACCEPTED);

        boolean isFollowing = connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(
                currentUser.getId(), userId, ConnectionStatus.ACCEPTED);
        boolean isFollowedBy = connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(
                userId, currentUser.getId(), ConnectionStatus.ACCEPTED);

        log.info("User {} has {} followers and {} following", userId, followersCount, followingCount);

        return ConnectionStatsResponse.builder()
                .userId(userId)
                .followersCount(followersCount)
                .followingCount(followingCount)
                .isFollowing(isFollowing)
                .isFollowedBy(isFollowedBy)
                .build();
    }

    public boolean isFollowing(Long userId) {
        User currentUser = authService.getCurrentUser();
        return connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(
                currentUser.getId(), userId, ConnectionStatus.ACCEPTED);
    }
}

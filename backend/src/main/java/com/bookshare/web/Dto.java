package com.bookshare.web;

import com.bookshare.domain.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** All request/response shapes of the REST API. */
public final class Dto {
    private Dto() { }

    public record CommunityDto(Long id, String name, String level, Long parentId, String path) { }
    public record CommunityReq(@NotBlank String name, @NotNull CommunityLevel level, Long parentId) { }

    public record UserSummary(Long id, String name, Long communityId, String communityName) { }
    public record UserDto(Long id, String name, String email, String role, String accountStatus, Instant restrictedUntil,
                          Long communityId, String communityName, double reputationScore, double complianceRatio) { }
    public record AuthResponse(String token, UserDto user) { }
    public record RegisterReq(@NotBlank @Size(max = 80) String name, @NotBlank @Email String email,
                              @NotBlank @Size(min = 8, max = 100) String password, @NotNull Long communityId) { }
    public record LoginReq(@NotBlank String email, @NotBlank String password) { }

    public record BookReq(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 200) String author,
                          String genre, String edition, @NotNull BookCondition condition,
                          @Size(max = 2000) String description, @NotNull ListingType listingType,
                          @DecimalMin("0.0") BigDecimal price, @Min(1) @Max(120) Integer lendingDurationDays,
                          Boolean available) { }
    public record BookDto(Long id, UserSummary owner, Long communityId, String communityName, String title, String author,
                          String genre, String edition, String condition, String coverUrl, String description,
                          String listingType, BigDecimal price, int lendingDurationDays, boolean available,
                          Instant createdAt, Long saleListingId, String saleStatus) { }

    public record ConditionCheckDto(Long id, Long bookId, Long requestId, String stage, List<String> photoUrls,
                                    UserSummary loggedBy, Instant createdAt, String aiNotes) { }

    public record ComplianceDto(long onTimeReturns, long totalBorrows, Double onTimeRatio,
                                long completedLends, long acceptedLends, Double lendRatio) { }

    public record CreateRequestReq(@NotNull Long bookId, @Size(max = 500) String note) { }
    public record RequestDto(Long id, BookDto book, UserSummary borrower, UserSummary owner, String status, String note,
                             Instant requestedAt, Instant acceptedAt, Instant pickedUpAt, Instant returnDueAt,
                             Instant returnedAt, Instant completedAt, Instant reminderSentAt,
                             Instant notReturnedReportableAt, boolean iRated, boolean theyRated,
                             ComplianceDto borrowerCompliance, ComplianceDto ownerCompliance) { }
    public record RequestDetailDto(RequestDto request, List<ConditionCheckDto> checks, List<ReportDto> reports,
                                   List<RatingDto> ratings) { }

    public record RatingReq(@Min(1) @Max(5) int communication, @Min(1) @Max(5) int care,
                            @Min(1) @Max(5) int reliability, @Size(max = 1000) String comment) { }
    public record RatingDto(Long id, Long requestId, UserSummary rater, UserSummary ratee, int communication, int care,
                            int reliability, String comment, Instant createdAt) { }

    public record ReportDto(Long id, Long requestId, Long bookId, String bookTitle, UserSummary reporter,
                            UserSummary reportedUser, String category, String status, String details,
                            List<String> evidencePhotoUrls, String adminNote, Instant createdAt, Instant resolvedAt,
                            List<ConditionCheckDto> checks) { }
    public record AdminReportReq(@NotNull ReportStatus status, @Size(max = 1000) String adminNote) { }
    public record StatusReq(@NotNull AccountStatus status) { }
    public record FlaggedUserDto(UserSummary user, String accountStatus, long validReports, long totalReports,
                                 Instant restrictedUntil) { }
    public record StatsDto(long users, long books, long openExchanges, long pendingReports, long activeSales) { }

    public record ProfileDto(UserSummary user, String bio, Instant memberSince, double reputationScore, long ratingCount,
                             double avgCommunication, double avgCare, double avgReliability, ComplianceDto compliance,
                             String accountStatus, List<RatingDto> recentRatings) { }

    public record SaleCreateReq(@NotNull Long bookId) { }
    public record ReserveReq(@NotNull Long buyerId, BigDecimal agreedPrice) { }
    public record SaleDto(Long id, BookDto book, UserSummary seller, UserSummary buyer, String status,
                          BigDecimal agreedPrice, Instant createdAt) { }
    public record ThreadDto(UserSummary buyer, String lastMessage, Instant lastAt) { }

    public record SendReq(@NotBlank @Size(max = 2000) String content, Long threadUserId) { }
    public record MessageDto(Long id, UserSummary sender, String content, boolean system, Instant sentAt, Long threadUserId) { }

    public record RoomReq(@NotBlank @Size(max = 120) String name, @Size(max = 80) String topic, RoomType type, Instant scheduledAt) { }
    public record RoomDto(Long id, String name, String topic, String type, Instant scheduledAt, UserSummary createdBy) { }
    public record ReadingRoomReq(@NotBlank @Size(max = 150) String title, @Size(max = 1000) String description,
                                 @NotNull Instant scheduledTime, @Min(2) @Max(12) int maxParticipants) { }
    public record ReadingRoomDto(Long id, String title, String description, Instant scheduledTime, int maxParticipants,
                                 UserSummary host, List<UserSummary> participants) { }
}

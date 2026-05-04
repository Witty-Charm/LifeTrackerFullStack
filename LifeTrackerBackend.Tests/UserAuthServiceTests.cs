using System.Security.Cryptography;
using LifeTracker.Configuration;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services.Auth;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace LifeTrackerBackend.Tests;

public class UserAuthServiceTests : IAsyncLifetime
{
    private readonly SqliteConnection _connection = new("Data Source=:memory:");
    private DbContextOptions<ApplicationDbContext> _options = null!;
    private MutableTimeProvider _time = null!;

    public async Task InitializeAsync()
    {
        await _connection.OpenAsync();
        _options = new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseSqlite(_connection)
            .Options;
        _time = new MutableTimeProvider(new DateTimeOffset(2026, 1, 15, 10, 0, 0, TimeSpan.Zero));

        await using var db = new ApplicationDbContext(_options);
        await db.Database.EnsureCreatedAsync();
    }

    public async Task DisposeAsync() => await _connection.DisposeAsync();

    private ApplicationDbContext CreateDb() => new(_options);

    private static AuthOptions BuildAuthOptions() => new()
    {
        JwtSigningKey = Convert.ToBase64String(RandomNumberGenerator.GetBytes(48)),
        JwtIssuer = "lifetracker-test",
        JwtAudience = "lifetracker-mobile-test",
        AccessTokenLifetime = TimeSpan.FromMinutes(15),
        RefreshTokenLifetime = TimeSpan.FromDays(30),
        GoogleWebClientId = "test-client-id",
    };

    private (UserAuthService service, ApplicationDbContext db, FakeGoogleVerifier google, JwtTokenService jwt)
        CreateService(GoogleUserInfo? defaultGoogleInfo = null)
    {
        var db = CreateDb();
        var google = new FakeGoogleVerifier
        {
            Result = defaultGoogleInfo ?? new GoogleUserInfo("google-sub-1", "alex@example.com", "Alex", "https://example.com/avatar.png"),
        };
        var jwt = new JwtTokenService(BuildAuthOptions(), _time);
        var service = new UserAuthService(db, google, jwt, _time);
        return (service, db, google, jwt);
    }

    [Fact]
    public async Task SignInWithGoogleAsync_creates_user_on_first_sign_in()
    {
        var (service, db, _, _) = CreateService();

        var result = await service.SignInWithGoogleAsync("any-id-token", null, default);

        Assert.NotEmpty(result.AccessToken);
        Assert.NotEmpty(result.RefreshToken);
        var user = await db.Users.SingleAsync();
        Assert.Equal(AuthProvider.Google, user.Provider);
        Assert.Equal("google-sub-1", user.ExternalId);
        Assert.Equal("alex@example.com", user.Email);
        Assert.Equal("Alex", user.DisplayName);
        Assert.Equal(result.User.Id, user.Id);
    }

    [Fact]
    public async Task SignInWithGoogleAsync_returns_existing_user_for_same_subject()
    {
        var (service, db, google, _) = CreateService();
        var first = await service.SignInWithGoogleAsync("token-1", null, default);

        google.Result = new GoogleUserInfo("google-sub-1", "alex.new@example.com", "Alex Renamed", null);
        _time.Advance(TimeSpan.FromHours(1));
        var second = await service.SignInWithGoogleAsync("token-2", null, default);

        Assert.Equal(first.User.Id, second.User.Id);
        Assert.Equal(1, await db.Users.CountAsync());
        var user = await db.Users.SingleAsync();
        Assert.Equal("alex.new@example.com", user.Email);
        Assert.Equal("Alex Renamed", user.DisplayName);
    }

    [Fact]
    public async Task SignInWithGoogleAsync_claims_orphan_hero_by_device_id()
    {
        var (service, db, _, _) = CreateService();
        var deviceId = Guid.NewGuid().ToString("D");
        db.Heroes.Add(new Hero { Name = "Alex", OwnerDeviceId = deviceId });
        await db.SaveChangesAsync();

        var result = await service.SignInWithGoogleAsync("token", deviceId, default);

        var hero = await db.Heroes.SingleAsync();
        Assert.Equal(result.User.Id, hero.UserId);
    }

    [Fact]
    public async Task SignInWithGoogleAsync_does_not_claim_hero_already_linked_to_other_user()
    {
        var (service, db, google, _) = CreateService();
        var deviceId = Guid.NewGuid().ToString("D");

        google.Result = new GoogleUserInfo("google-sub-A", "a@x", "A", null);
        var firstUser = await service.SignInWithGoogleAsync("t", null, default);
        db.Heroes.Add(new Hero { Name = "Hero", OwnerDeviceId = deviceId, UserId = firstUser.User.Id });
        await db.SaveChangesAsync();

        google.Result = new GoogleUserInfo("google-sub-B", "b@x", "B", null);
        var secondUser = await service.SignInWithGoogleAsync("t", deviceId, default);

        var hero = await db.Heroes.SingleAsync();
        Assert.Equal(firstUser.User.Id, hero.UserId);
        Assert.NotEqual(secondUser.User.Id, hero.UserId);
    }

    [Fact]
    public async Task SignInWithGoogleAsync_persists_refresh_token_hash_only()
    {
        var (service, db, _, jwt) = CreateService();

        var result = await service.SignInWithGoogleAsync("t", null, default);

        var stored = await db.RefreshTokens.SingleAsync();
        Assert.NotEqual(result.RefreshToken, stored.TokenHash);
        Assert.Equal(jwt.HashRefreshToken(result.RefreshToken), stored.TokenHash);
        Assert.Null(stored.RevokedAt);
    }

    [Fact]
    public async Task RefreshAsync_rotates_token_and_revokes_previous()
    {
        var (service, db, _, jwt) = CreateService();
        var first = await service.SignInWithGoogleAsync("t", null, default);

        _time.Advance(TimeSpan.FromMinutes(20));
        var second = await service.RefreshAsync(first.RefreshToken, deviceId: null, default);

        Assert.NotEqual(first.RefreshToken, second.RefreshToken);
        Assert.NotEqual(first.AccessToken, second.AccessToken);

        var oldHash = jwt.HashRefreshToken(first.RefreshToken);
        var newHash = jwt.HashRefreshToken(second.RefreshToken);
        var oldToken = await db.RefreshTokens.SingleAsync(t => t.TokenHash == oldHash);
        var newToken = await db.RefreshTokens.SingleAsync(t => t.TokenHash == newHash);

        Assert.NotNull(oldToken.RevokedAt);
        Assert.Equal(newHash, oldToken.ReplacedByTokenHash);
        Assert.Null(newToken.RevokedAt);
    }

    [Fact]
    public async Task RefreshAsync_reusing_revoked_token_revokes_entire_chain()
    {
        var (service, db, _, _) = CreateService();
        var first = await service.SignInWithGoogleAsync("t", null, default);
        var second = await service.RefreshAsync(first.RefreshToken, null, default);

        await Assert.ThrowsAsync<AuthenticationFailedException>(
            () => service.RefreshAsync(first.RefreshToken, null, default));

        Assert.True(await db.RefreshTokens.AllAsync(t => t.RevokedAt != null));
    }

    [Fact]
    public async Task RefreshAsync_with_unknown_token_throws()
    {
        var (service, _, _, _) = CreateService();
        _ = await service.SignInWithGoogleAsync("t", null, default);

        await Assert.ThrowsAsync<AuthenticationFailedException>(
            () => service.RefreshAsync("totally-not-a-real-token", null, default));
    }

    [Fact]
    public async Task RefreshAsync_with_expired_token_throws_and_marks_revoked()
    {
        var (service, db, _, jwt) = CreateService();
        var first = await service.SignInWithGoogleAsync("t", null, default);

        _time.Advance(TimeSpan.FromDays(31));

        await Assert.ThrowsAsync<AuthenticationFailedException>(
            () => service.RefreshAsync(first.RefreshToken, null, default));

        var hash = jwt.HashRefreshToken(first.RefreshToken);
        var stored = await db.RefreshTokens.SingleAsync(t => t.TokenHash == hash);
        Assert.NotNull(stored.RevokedAt);
    }

    [Fact]
    public async Task SignOutAsync_with_specific_token_revokes_only_that_token()
    {
        var (service, db, _, jwt) = CreateService();
        var first = await service.SignInWithGoogleAsync("t", null, default);
        // Issue a second refresh in parallel by signing in again from another device
        var second = await service.SignInWithGoogleAsync("t", "11111111-1111-1111-1111-111111111111", default);

        await service.SignOutAsync(first.User.Id, first.RefreshToken, default);

        var firstHash = jwt.HashRefreshToken(first.RefreshToken);
        var secondHash = jwt.HashRefreshToken(second.RefreshToken);
        Assert.NotNull((await db.RefreshTokens.SingleAsync(t => t.TokenHash == firstHash)).RevokedAt);
        Assert.Null((await db.RefreshTokens.SingleAsync(t => t.TokenHash == secondHash)).RevokedAt);
    }

    [Fact]
    public async Task SignOutAsync_without_token_revokes_all_user_tokens()
    {
        var (service, db, _, _) = CreateService();
        var first = await service.SignInWithGoogleAsync("t", null, default);
        _ = await service.SignInWithGoogleAsync("t", "11111111-1111-1111-1111-111111111111", default);

        await service.SignOutAsync(first.User.Id, refreshToken: null, default);

        Assert.True(await db.RefreshTokens.AllAsync(t => t.RevokedAt != null));
    }

    [Fact]
    public async Task SignInWithGoogleAsync_propagates_google_verification_error()
    {
        var (service, _, google, _) = CreateService();
        google.ThrowOnVerify = new GoogleTokenVerificationException("bad token");

        await Assert.ThrowsAsync<GoogleTokenVerificationException>(
            () => service.SignInWithGoogleAsync("t", null, default));
    }

    private sealed class FakeGoogleVerifier : IGoogleTokenVerifier
    {
        public GoogleUserInfo Result { get; set; } = new("sub", "e@x", "n", null);
        public Exception? ThrowOnVerify { get; set; }

        public Task<GoogleUserInfo> VerifyAsync(string idToken, CancellationToken cancellationToken)
        {
            if (ThrowOnVerify is not null) throw ThrowOnVerify;
            return Task.FromResult(Result);
        }
    }

    private sealed class MutableTimeProvider : TimeProvider
    {
        private DateTimeOffset _now;
        public MutableTimeProvider(DateTimeOffset start) => _now = start;
        public override DateTimeOffset GetUtcNow() => _now;
        public void Advance(TimeSpan delta) => _now = _now.Add(delta);
    }
}

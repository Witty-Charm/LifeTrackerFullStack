using LifeTracker.Data;
using LifeTracker.Models;
using Microsoft.EntityFrameworkCore;

namespace LifeTracker.Services.Auth;

public sealed class UserAuthService : IUserAuthService
{
    private readonly ApplicationDbContext _db;
    private readonly IGoogleTokenVerifier _googleVerifier;
    private readonly IJwtTokenService _jwt;
    private readonly TimeProvider _time;

    public UserAuthService(
        ApplicationDbContext db,
        IGoogleTokenVerifier googleVerifier,
        IJwtTokenService jwt,
        TimeProvider time)
    {
        _db = db;
        _googleVerifier = googleVerifier;
        _jwt = jwt;
        _time = time;
    }

    public async Task<AuthResult> SignInWithGoogleAsync(
        string googleIdToken,
        string? claimDeviceId,
        CancellationToken ct)
    {
        var info = await _googleVerifier.VerifyAsync(googleIdToken, ct);
        var now = _time.GetUtcNow();

        var user = await _db.Users
            .FirstOrDefaultAsync(u => u.Provider == AuthProvider.Google && u.ExternalId == info.Subject, ct);

        if (user is null)
        {
            user = new User
            {
                Provider = AuthProvider.Google,
                ExternalId = info.Subject,
                Email = info.Email,
                DisplayName = info.Name,
                AvatarUrl = info.Picture,
                CreatedAt = now,
                LastLoginAt = now,
            };
            _db.Users.Add(user);
            await _db.SaveChangesAsync(ct);
        }
        else
        {
            user.Email = info.Email;
            user.DisplayName = info.Name;
            user.AvatarUrl = info.Picture;
            user.LastLoginAt = now;
        }

        await TryClaimHeroByDeviceIdAsync(user, claimDeviceId, ct);

        return await IssueTokensAsync(user, claimDeviceId, ct);
    }

    public async Task<AuthResult> RefreshAsync(string refreshToken, string? deviceId, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(refreshToken))
            throw new AuthenticationFailedException("Refresh token is required.");

        var hash = _jwt.HashRefreshToken(refreshToken);
        var token = await _db.RefreshTokens.FirstOrDefaultAsync(t => t.TokenHash == hash, ct);
        if (token is null)
            throw new AuthenticationFailedException("Refresh token is not recognized.");

        var now = _time.GetUtcNow();

        if (token.RevokedAt is not null)
        {
            // Re-use of an already-revoked token: revoke the entire chain belonging to this user
            // as a defensive measure (token theft suspicion).
            await RevokeAllUserTokensAsync(token.UserId, now, ct);
            throw new AuthenticationFailedException("Refresh token has been revoked.");
        }

        if (now >= token.ExpiresAt)
        {
            token.RevokedAt = now;
            await _db.SaveChangesAsync(ct);
            throw new AuthenticationFailedException("Refresh token has expired.");
        }

        var user = await _db.Users.FirstOrDefaultAsync(u => u.Id == token.UserId, ct)
            ?? throw new AuthenticationFailedException("User no longer exists.");

        var newRefresh = _jwt.IssueRefreshToken();
        token.RevokedAt = now;
        token.ReplacedByTokenHash = newRefresh.Hash;

        var newRecord = new RefreshToken
        {
            UserId = user.Id,
            TokenHash = newRefresh.Hash,
            CreatedAt = now,
            ExpiresAt = newRefresh.ExpiresAt,
            DeviceId = deviceId ?? token.DeviceId,
        };
        _db.RefreshTokens.Add(newRecord);

        user.LastLoginAt = now;

        await _db.SaveChangesAsync(ct);

        var access = _jwt.IssueAccessToken(user);
        return new AuthResult(
            access.Token, access.ExpiresAt,
            newRefresh.PlainToken, newRefresh.ExpiresAt,
            ToAuthenticatedUser(user));
    }

    public async Task SignOutAsync(int userId, string? refreshToken, CancellationToken ct)
    {
        var now = _time.GetUtcNow();

        if (!string.IsNullOrWhiteSpace(refreshToken))
        {
            var hash = _jwt.HashRefreshToken(refreshToken);
            var token = await _db.RefreshTokens.FirstOrDefaultAsync(t => t.TokenHash == hash && t.UserId == userId, ct);
            if (token is { RevokedAt: null })
            {
                token.RevokedAt = now;
                await _db.SaveChangesAsync(ct);
            }
            return;
        }

        await RevokeAllUserTokensAsync(userId, now, ct);
    }

    private async Task RevokeAllUserTokensAsync(int userId, DateTimeOffset now, CancellationToken ct)
    {
        var active = await _db.RefreshTokens
            .Where(t => t.UserId == userId && t.RevokedAt == null)
            .ToListAsync(ct);

        if (active.Count == 0) return;

        foreach (var t in active)
            t.RevokedAt = now;

        await _db.SaveChangesAsync(ct);
    }

    private async Task TryClaimHeroByDeviceIdAsync(User user, string? deviceId, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(deviceId)) return;

        var orphans = await _db.Heroes
            .Where(h => h.OwnerDeviceId == deviceId && h.UserId == null)
            .ToListAsync(ct);

        if (orphans.Count == 0) return;

        foreach (var hero in orphans)
            hero.UserId = user.Id;

        await _db.SaveChangesAsync(ct);
    }

    private async Task<AuthResult> IssueTokensAsync(User user, string? deviceId, CancellationToken ct)
    {
        var access = _jwt.IssueAccessToken(user);
        var refresh = _jwt.IssueRefreshToken();

        var now = _time.GetUtcNow();
        _db.RefreshTokens.Add(new RefreshToken
        {
            UserId = user.Id,
            TokenHash = refresh.Hash,
            CreatedAt = now,
            ExpiresAt = refresh.ExpiresAt,
            DeviceId = deviceId,
        });

        await _db.SaveChangesAsync(ct);

        return new AuthResult(
            access.Token, access.ExpiresAt,
            refresh.PlainToken, refresh.ExpiresAt,
            ToAuthenticatedUser(user));
    }

    private static AuthenticatedUser ToAuthenticatedUser(User user) =>
        new(user.Id, user.Email, user.DisplayName, user.AvatarUrl);
}

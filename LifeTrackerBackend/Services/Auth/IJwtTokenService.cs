using LifeTracker.Models;

namespace LifeTracker.Services.Auth;

public interface IJwtTokenService
{
    AccessTokenResult IssueAccessToken(User user);

    RefreshTokenResult IssueRefreshToken();

    string HashRefreshToken(string plainToken);
}

public sealed record AccessTokenResult(string Token, DateTimeOffset ExpiresAt);

public sealed record RefreshTokenResult(string PlainToken, string Hash, DateTimeOffset ExpiresAt);

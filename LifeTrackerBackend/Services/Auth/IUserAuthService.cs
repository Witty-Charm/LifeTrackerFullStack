using LifeTracker.Models;

namespace LifeTracker.Services.Auth;

public interface IUserAuthService
{
    Task<AuthResult> SignInWithGoogleAsync(
        string googleIdToken,
        string? claimDeviceId,
        CancellationToken cancellationToken);

    Task<AuthResult> RefreshAsync(
        string refreshToken,
        string? deviceId,
        CancellationToken cancellationToken);

    Task SignOutAsync(int userId, string? refreshToken, CancellationToken cancellationToken);
}

public sealed record AuthResult(
    string AccessToken,
    DateTimeOffset AccessTokenExpiresAt,
    string RefreshToken,
    DateTimeOffset RefreshTokenExpiresAt,
    AuthenticatedUser User);

public sealed record AuthenticatedUser(
    int Id,
    string Email,
    string? DisplayName,
    string? AvatarUrl);

public sealed class AuthenticationFailedException : Exception
{
    public AuthenticationFailedException(string message) : base(message) { }
    public AuthenticationFailedException(string message, Exception inner) : base(message, inner) { }
}

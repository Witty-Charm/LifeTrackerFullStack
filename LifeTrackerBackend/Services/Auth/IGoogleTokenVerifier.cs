namespace LifeTracker.Services.Auth;

public interface IGoogleTokenVerifier
{
    Task<GoogleUserInfo> VerifyAsync(string idToken, CancellationToken cancellationToken);
}

public sealed record GoogleUserInfo(
    string Subject,
    string Email,
    string? Name,
    string? Picture);

public sealed class GoogleTokenVerificationException : Exception
{
    public GoogleTokenVerificationException(string message) : base(message) { }
    public GoogleTokenVerificationException(string message, Exception inner) : base(message, inner) { }
}

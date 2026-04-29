using Google.Apis.Auth;
using LifeTracker.Configuration;

namespace LifeTracker.Services.Auth;

public sealed class GoogleTokenVerifier : IGoogleTokenVerifier
{
    private readonly AuthOptions _options;

    public GoogleTokenVerifier(AuthOptions options)
    {
        _options = options;
    }

    public async Task<GoogleUserInfo> VerifyAsync(string idToken, CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(idToken))
            throw new GoogleTokenVerificationException("Google ID token is empty.");

        if (string.IsNullOrWhiteSpace(_options.GoogleWebClientId))
            throw new GoogleTokenVerificationException("Google Web client id is not configured on the server.");

        var settings = new GoogleJsonWebSignature.ValidationSettings
        {
            Audience = new[] { _options.GoogleWebClientId },
        };

        GoogleJsonWebSignature.Payload payload;
        try
        {
            payload = await GoogleJsonWebSignature.ValidateAsync(idToken, settings).WaitAsync(cancellationToken);
        }
        catch (InvalidJwtException ex)
        {
            throw new GoogleTokenVerificationException("Google ID token failed validation.", ex);
        }

        if (string.IsNullOrWhiteSpace(payload.Subject))
            throw new GoogleTokenVerificationException("Google ID token has no subject claim.");

        if (string.IsNullOrWhiteSpace(payload.Email))
            throw new GoogleTokenVerificationException("Google ID token has no email claim.");

        if (payload.EmailVerified is false)
            throw new GoogleTokenVerificationException("Google account email is not verified.");

        return new GoogleUserInfo(payload.Subject, payload.Email, payload.Name, payload.Picture);
    }
}

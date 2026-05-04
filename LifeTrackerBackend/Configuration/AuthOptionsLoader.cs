using Microsoft.Extensions.Configuration;

namespace LifeTracker.Configuration;

public static class AuthOptionsLoader
{
    public static AuthOptions Load(IConfiguration configuration)
    {
        var options = new AuthOptions();
        configuration.GetSection(AuthOptions.SectionName).Bind(options);

        var envSigningKey = configuration["LIFETRACKER_JWT_KEY"];
        if (!string.IsNullOrWhiteSpace(envSigningKey))
            options.JwtSigningKey = envSigningKey;

        var envIssuer = configuration["LIFETRACKER_JWT_ISSUER"];
        if (!string.IsNullOrWhiteSpace(envIssuer))
            options.JwtIssuer = envIssuer;

        var envAudience = configuration["LIFETRACKER_JWT_AUDIENCE"];
        if (!string.IsNullOrWhiteSpace(envAudience))
            options.JwtAudience = envAudience;

        var envGoogleClientId = configuration["LIFETRACKER_GOOGLE_WEB_CLIENT_ID"];
        if (!string.IsNullOrWhiteSpace(envGoogleClientId))
            options.GoogleWebClientId = envGoogleClientId;

        if (string.IsNullOrWhiteSpace(options.JwtSigningKey))
        {
            throw new InvalidOperationException(
                "JWT signing key is not configured. Set Auth:JwtSigningKey or LIFETRACKER_JWT_KEY env variable to a base64 string of at least 32 bytes.");
        }

        return options;
    }
}

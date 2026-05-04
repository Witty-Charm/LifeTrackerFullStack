namespace LifeTracker.Configuration;

public class AuthOptions
{
    public const string SectionName = "Auth";

    public string JwtSigningKey { get; set; } = string.Empty;
    public string JwtIssuer { get; set; } = "lifetracker";
    public string JwtAudience { get; set; } = "lifetracker-mobile";

    public TimeSpan AccessTokenLifetime { get; set; } = TimeSpan.FromMinutes(15);
    public TimeSpan RefreshTokenLifetime { get; set; } = TimeSpan.FromDays(30);

    public string GoogleWebClientId { get; set; } = string.Empty;
}

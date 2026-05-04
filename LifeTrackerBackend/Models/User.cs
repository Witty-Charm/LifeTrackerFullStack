namespace LifeTracker.Models;

public class User
{
    public int Id { get; set; }

    public AuthProvider Provider { get; set; } = AuthProvider.Google;
    public string ExternalId { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;
    public string? DisplayName { get; set; }
    public string? AvatarUrl { get; set; }

    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset LastLoginAt { get; set; } = DateTimeOffset.UtcNow;

    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public ICollection<Hero> Heroes { get; set; } = new List<Hero>();
    public ICollection<RefreshToken> RefreshTokens { get; set; } = new List<RefreshToken>();
}

public enum AuthProvider
{
    Google = 1,
}

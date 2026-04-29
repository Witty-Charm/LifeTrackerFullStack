using System.IdentityModel.Tokens.Jwt;
using System.Security.Cryptography;
using System.Text;
using LifeTracker.Configuration;
using LifeTracker.Models;
using LifeTracker.Services.Auth;
using Microsoft.IdentityModel.Tokens;

namespace LifeTrackerBackend.Tests;

public class JwtTokenServiceTests
{
    private static AuthOptions DefaultOptions() => new()
    {
        JwtSigningKey = Convert.ToBase64String(RandomNumberGenerator.GetBytes(48)),
        JwtIssuer = "lifetracker-test",
        JwtAudience = "lifetracker-mobile-test",
        AccessTokenLifetime = TimeSpan.FromMinutes(15),
        RefreshTokenLifetime = TimeSpan.FromDays(30),
        GoogleWebClientId = "test-client-id",
    };

    [Fact]
    public void IssueAccessToken_includes_required_claims_and_lifetime()
    {
        var fixedNow = new DateTimeOffset(2026, 1, 1, 12, 0, 0, TimeSpan.Zero);
        var time = new FakeTimeProvider(fixedNow);
        var options = DefaultOptions();
        var service = new JwtTokenService(options, time);
        var user = new User
        {
            Id = 42,
            Email = "alex@example.com",
            DisplayName = "Alex",
            Provider = AuthProvider.Google,
            ExternalId = "google-sub-123",
        };

        var result = service.IssueAccessToken(user);

        Assert.Equal(fixedNow + options.AccessTokenLifetime, result.ExpiresAt);

        var jwt = new JwtSecurityTokenHandler().ReadJwtToken(result.Token);
        Assert.Equal(options.JwtIssuer, jwt.Issuer);
        Assert.Contains(options.JwtAudience, jwt.Audiences);
        Assert.Equal("42", jwt.Claims.First(c => c.Type == JwtRegisteredClaimNames.Sub).Value);
        Assert.Equal("alex@example.com", jwt.Claims.First(c => c.Type == JwtRegisteredClaimNames.Email).Value);
        Assert.Equal("Alex", jwt.Claims.First(c => c.Type == JwtRegisteredClaimNames.Name).Value);
        Assert.NotNull(jwt.Claims.FirstOrDefault(c => c.Type == JwtRegisteredClaimNames.Jti));
        Assert.Equal(fixedNow.UtcDateTime, jwt.ValidFrom);
        Assert.Equal((fixedNow + options.AccessTokenLifetime).UtcDateTime, jwt.ValidTo);
    }

    [Fact]
    public void IssueAccessToken_token_validates_against_configured_parameters()
    {
        var options = DefaultOptions();
        var time = new FakeTimeProvider(DateTimeOffset.UtcNow);
        var service = new JwtTokenService(options, time);
        var user = new User { Id = 7, Email = "u@e.com", Provider = AuthProvider.Google, ExternalId = "g-7" };

        var result = service.IssueAccessToken(user);

        var keyBytes = Convert.FromBase64String(options.JwtSigningKey);
        var parameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = options.JwtIssuer,
            ValidateAudience = true,
            ValidAudience = options.JwtAudience,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(keyBytes),
            ValidateLifetime = true,
            ClockSkew = TimeSpan.Zero,
        };

        var handler = new JwtSecurityTokenHandler();
        handler.InboundClaimTypeMap.Clear();
        var principal = handler.ValidateToken(result.Token, parameters, out _);
        Assert.Equal("7", principal.FindFirst(JwtRegisteredClaimNames.Sub)?.Value);
    }

    [Fact]
    public void IssueRefreshToken_returns_distinct_high_entropy_tokens()
    {
        var service = new JwtTokenService(DefaultOptions());

        var a = service.IssueRefreshToken();
        var b = service.IssueRefreshToken();

        Assert.NotEqual(a.PlainToken, b.PlainToken);
        Assert.NotEqual(a.Hash, b.Hash);
        Assert.True(a.PlainToken.Length >= 40, $"Refresh token too short: {a.PlainToken.Length}");
    }

    [Fact]
    public void HashRefreshToken_is_deterministic_and_matches_issued_hash()
    {
        var service = new JwtTokenService(DefaultOptions());

        var issued = service.IssueRefreshToken();
        var rehashed = service.HashRefreshToken(issued.PlainToken);

        Assert.Equal(issued.Hash, rehashed);
        Assert.Equal(64, rehashed.Length);
    }

    [Fact]
    public void HashRefreshToken_differs_for_different_inputs()
    {
        var service = new JwtTokenService(DefaultOptions());

        var h1 = service.HashRefreshToken("abc");
        var h2 = service.HashRefreshToken("abd");

        Assert.NotEqual(h1, h2);
    }

    [Fact]
    public void Constructor_throws_when_signing_key_is_too_short()
    {
        var options = DefaultOptions();
        options.JwtSigningKey = Convert.ToBase64String(new byte[16]);

        Assert.Throws<InvalidOperationException>(() => new JwtTokenService(options));
    }

    [Fact]
    public void Constructor_throws_when_signing_key_is_empty()
    {
        var options = DefaultOptions();
        options.JwtSigningKey = string.Empty;

        Assert.Throws<InvalidOperationException>(() => new JwtTokenService(options));
    }

    [Fact]
    public void Constructor_accepts_plain_text_signing_key_when_long_enough()
    {
        var options = DefaultOptions();
        options.JwtSigningKey = "this-is-a-plain-text-signing-key-that-is-long-enough-to-work-as-256-bits";
        Assert.True(Encoding.UTF8.GetBytes(options.JwtSigningKey).Length >= 32);

        var service = new JwtTokenService(options);
        var token = service.IssueAccessToken(new User { Id = 1, Email = "x@x", Provider = AuthProvider.Google, ExternalId = "g" });
        Assert.NotEmpty(token.Token);
    }

    private sealed class FakeTimeProvider : TimeProvider
    {
        private readonly DateTimeOffset _now;
        public FakeTimeProvider(DateTimeOffset now) => _now = now;
        public override DateTimeOffset GetUtcNow() => _now;
    }
}

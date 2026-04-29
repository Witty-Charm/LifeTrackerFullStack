using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using LifeTracker.Configuration;
using LifeTracker.Models;
using Microsoft.IdentityModel.Tokens;

namespace LifeTracker.Services.Auth;

public sealed class JwtTokenService : IJwtTokenService
{
    private const int RefreshTokenByteLength = 32;

    private readonly AuthOptions _options;
    private readonly SigningCredentials _signingCredentials;
    private readonly TimeProvider _timeProvider;

    public JwtTokenService(AuthOptions options, TimeProvider? timeProvider = null)
    {
        _options = options;
        _timeProvider = timeProvider ?? TimeProvider.System;

        var keyBytes = DecodeSigningKey(options.JwtSigningKey);
        if (keyBytes.Length < 32)
        {
            throw new InvalidOperationException(
                "JWT signing key must be at least 32 bytes (256 bits) when decoded.");
        }

        _signingCredentials = new SigningCredentials(
            new SymmetricSecurityKey(keyBytes),
            SecurityAlgorithms.HmacSha256);
    }

    public AccessTokenResult IssueAccessToken(User user)
    {
        var now = _timeProvider.GetUtcNow();
        var expiresAt = now + _options.AccessTokenLifetime;

        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
            new(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString("D")),
            new(JwtRegisteredClaimNames.Iat, now.ToUnixTimeSeconds().ToString(), ClaimValueTypes.Integer64),
        };

        if (!string.IsNullOrWhiteSpace(user.Email))
            claims.Add(new Claim(JwtRegisteredClaimNames.Email, user.Email));

        if (!string.IsNullOrWhiteSpace(user.DisplayName))
            claims.Add(new Claim(JwtRegisteredClaimNames.Name, user.DisplayName));

        var token = new JwtSecurityToken(
            issuer: _options.JwtIssuer,
            audience: _options.JwtAudience,
            claims: claims,
            notBefore: now.UtcDateTime,
            expires: expiresAt.UtcDateTime,
            signingCredentials: _signingCredentials);

        var encoded = new JwtSecurityTokenHandler().WriteToken(token);
        return new AccessTokenResult(encoded, expiresAt);
    }

    public RefreshTokenResult IssueRefreshToken()
    {
        var bytes = new byte[RefreshTokenByteLength];
        RandomNumberGenerator.Fill(bytes);
        var plain = Base64UrlEncoder.Encode(bytes);
        var hash = HashRefreshToken(plain);
        var expiresAt = _timeProvider.GetUtcNow() + _options.RefreshTokenLifetime;
        return new RefreshTokenResult(plain, hash, expiresAt);
    }

    public string HashRefreshToken(string plainToken)
    {
        if (string.IsNullOrEmpty(plainToken))
            throw new ArgumentException("Refresh token must be provided.", nameof(plainToken));

        var bytes = Encoding.UTF8.GetBytes(plainToken);
        var hashBytes = SHA256.HashData(bytes);
        return Convert.ToHexString(hashBytes);
    }

    private static byte[] DecodeSigningKey(string raw)
    {
        if (string.IsNullOrWhiteSpace(raw))
            throw new InvalidOperationException("JWT signing key is empty.");

        try
        {
            return Convert.FromBase64String(raw);
        }
        catch (FormatException)
        {
            return Encoding.UTF8.GetBytes(raw);
        }
    }
}

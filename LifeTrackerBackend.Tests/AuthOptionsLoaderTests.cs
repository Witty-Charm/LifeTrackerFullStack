using LifeTracker.Configuration;
using Microsoft.Extensions.Configuration;

namespace LifeTrackerBackend.Tests;

public class AuthOptionsLoaderTests
{
    private static IConfiguration BuildConfiguration(IDictionary<string, string?> values) =>
        new ConfigurationBuilder().AddInMemoryCollection(values).Build();

    [Fact]
    public void Load_reads_values_from_configuration_section()
    {
        var config = BuildConfiguration(new Dictionary<string, string?>
        {
            ["Auth:JwtSigningKey"] = "Zm9v",
            ["Auth:JwtIssuer"] = "issuer-from-config",
            ["Auth:JwtAudience"] = "audience-from-config",
            ["Auth:GoogleWebClientId"] = "google-client-from-config",
        });

        var options = AuthOptionsLoader.Load(config);

        Assert.Equal("Zm9v", options.JwtSigningKey);
        Assert.Equal("issuer-from-config", options.JwtIssuer);
        Assert.Equal("audience-from-config", options.JwtAudience);
        Assert.Equal("google-client-from-config", options.GoogleWebClientId);
    }

    [Fact]
    public void Load_env_variables_override_configuration_section()
    {
        var config = BuildConfiguration(new Dictionary<string, string?>
        {
            ["Auth:JwtSigningKey"] = "from-section",
            ["Auth:JwtIssuer"] = "from-section",
            ["Auth:JwtAudience"] = "from-section",
            ["Auth:GoogleWebClientId"] = "from-section",
            ["LIFETRACKER_JWT_KEY"] = "from-env",
            ["LIFETRACKER_JWT_ISSUER"] = "issuer-from-env",
            ["LIFETRACKER_JWT_AUDIENCE"] = "audience-from-env",
            ["LIFETRACKER_GOOGLE_WEB_CLIENT_ID"] = "google-from-env",
        });

        var options = AuthOptionsLoader.Load(config);

        Assert.Equal("from-env", options.JwtSigningKey);
        Assert.Equal("issuer-from-env", options.JwtIssuer);
        Assert.Equal("audience-from-env", options.JwtAudience);
        Assert.Equal("google-from-env", options.GoogleWebClientId);
    }

    [Fact]
    public void Load_throws_when_signing_key_is_missing()
    {
        var config = BuildConfiguration(new Dictionary<string, string?>());

        Assert.Throws<InvalidOperationException>(() => AuthOptionsLoader.Load(config));
    }

    [Fact]
    public void Load_returns_default_lifetimes_when_not_overridden()
    {
        var config = BuildConfiguration(new Dictionary<string, string?>
        {
            ["Auth:JwtSigningKey"] = "Zm9v",
        });

        var options = AuthOptionsLoader.Load(config);

        Assert.Equal(TimeSpan.FromMinutes(15), options.AccessTokenLifetime);
        Assert.Equal(TimeSpan.FromDays(30), options.RefreshTokenLifetime);
        Assert.Equal("lifetracker", options.JwtIssuer);
        Assert.Equal("lifetracker-mobile", options.JwtAudience);
    }
}

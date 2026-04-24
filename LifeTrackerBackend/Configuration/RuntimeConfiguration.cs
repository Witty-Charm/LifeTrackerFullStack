using Microsoft.Extensions.Configuration;
using Npgsql;
using System.Collections;

namespace LifeTracker.Configuration;

public static class RuntimeConfiguration
{
    public static string ResolveConnectionString(IConfiguration configuration)
    {
        var explicitConnectionString = configuration.GetConnectionString("DefaultConnection");
        if (!string.IsNullOrWhiteSpace(explicitConnectionString))
        {
            return explicitConnectionString;
        }

        var databaseUrl = configuration["DATABASE_URL"];
        if (string.IsNullOrWhiteSpace(databaseUrl))
        {
            throw new InvalidOperationException("No database connection string was configured. Set ConnectionStrings__DefaultConnection or DATABASE_URL.");
        }

        return ConvertDatabaseUrlToNpgsqlConnectionString(databaseUrl);
    }

    public static string ResolveListenUrl(IDictionary environment)
    {
        var port = environment["PORT"]?.ToString();
        if (string.IsNullOrWhiteSpace(port))
        {
            port = "5000";
        }

        return $"http://0.0.0.0:{port}";
    }

    private static string ConvertDatabaseUrlToNpgsqlConnectionString(string databaseUrl)
    {
        var normalized = databaseUrl.Replace("postgres://", "postgresql://", StringComparison.OrdinalIgnoreCase);
        var uri = new Uri(normalized);

        var userInfo = uri.UserInfo.Split(':', 2);
        var builder = new NpgsqlConnectionStringBuilder
        {
            Host = uri.Host,
            Port = uri.Port,
            Database = uri.AbsolutePath.TrimStart('/'),
            Username = Uri.UnescapeDataString(userInfo[0]),
            Password = userInfo.Length > 1 ? Uri.UnescapeDataString(userInfo[1]) : string.Empty,
            SslMode = SslMode.Prefer,
        };

        return builder.ConnectionString;
    }
}

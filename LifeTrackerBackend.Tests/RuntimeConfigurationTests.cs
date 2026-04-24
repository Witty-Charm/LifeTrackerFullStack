using System.Collections;
using Microsoft.Extensions.Configuration;

namespace LifeTrackerBackend.Tests;

public class RuntimeConfigurationTests
{
    [Fact]
    public void ResolveConnectionString_UsesExplicitConnectionStringFirst()
    {
        var configuration = BuildConfiguration(new Dictionary<string, string?>
        {
            ["ConnectionStrings:DefaultConnection"] = "Host=localhost;Port=5432;Database=lifetracker;Username=postgres;Password=postgres",
            ["DATABASE_URL"] = "postgresql://ignored:ignored@localhost:5432/ignored"
        });

        var value = LifeTracker.Configuration.RuntimeConfiguration.ResolveConnectionString(configuration);

        Assert.Equal("Host=localhost;Port=5432;Database=lifetracker;Username=postgres;Password=postgres", value);
    }

    [Theory]
    [InlineData("postgresql://postgres:secret@db.railway.internal:5432/railway")]
    [InlineData("postgres://postgres:secret@db.railway.internal:5432/railway")]
    public void ResolveConnectionString_MapsDatabaseUrlToNpgsqlFormat(string databaseUrl)
    {
        var configuration = BuildConfiguration(new Dictionary<string, string?>
        {
            ["DATABASE_URL"] = databaseUrl
        });

        var value = LifeTracker.Configuration.RuntimeConfiguration.ResolveConnectionString(configuration);

        Assert.Contains("Host=db.railway.internal", value);
        Assert.Contains("Port=5432", value);
        Assert.Contains("Database=railway", value);
        Assert.Contains("Username=postgres", value);
        Assert.Contains("Password=secret", value);
    }

    [Fact]
    public void ResolveListenUrl_UsesPortEnvironmentVariable()
    {
        var environment = new DictionaryEnvironment(new Dictionary<string, string?>
        {
            ["PORT"] = "8080"
        });

        var value = LifeTracker.Configuration.RuntimeConfiguration.ResolveListenUrl(environment);

        Assert.Equal("http://0.0.0.0:8080", value);
    }

    [Fact]
    public void ResolveListenUrl_FallsBackTo5000()
    {
        var value = LifeTracker.Configuration.RuntimeConfiguration.ResolveListenUrl(new DictionaryEnvironment(new Dictionary<string, string?>()));

        Assert.Equal("http://0.0.0.0:5000", value);
    }

    private static IConfiguration BuildConfiguration(Dictionary<string, string?> values) =>
        new ConfigurationBuilder()
            .AddInMemoryCollection(values)
            .Build();

    private sealed class DictionaryEnvironment : IDictionary
    {
        private readonly Dictionary<string, string?> _values;

        public DictionaryEnvironment(Dictionary<string, string?> values)
        {
            _values = values;
        }

        public object? this[object key]
        {
            get => key is string s && _values.TryGetValue(s, out var value) ? value : null;
            set => throw new NotSupportedException();
        }

        public bool IsFixedSize => false;
        public bool IsReadOnly => true;
        public ICollection Keys => _values.Keys.ToList();
        public ICollection Values => _values.Values.ToList();
        public int Count => _values.Count;
        public object SyncRoot => this;
        public bool IsSynchronized => false;
        public void Add(object key, object? value) => throw new NotSupportedException();
        public void Clear() => throw new NotSupportedException();
        public bool Contains(object key) => key is string s && _values.ContainsKey(s);
        public void CopyTo(Array array, int index) => throw new NotSupportedException();
        public IDictionaryEnumerator GetEnumerator() => ((IDictionary)_values.ToDictionary(x => x.Key, x => (object?)x.Value)).GetEnumerator();
        public void Remove(object key) => throw new NotSupportedException();
        IEnumerator IEnumerable.GetEnumerator() => _values.GetEnumerator();
    }
}

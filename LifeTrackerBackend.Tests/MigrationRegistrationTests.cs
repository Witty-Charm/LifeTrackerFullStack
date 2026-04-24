using LifeTracker.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;

namespace LifeTrackerBackend.Tests;

public class MigrationRegistrationTests
{
    [Fact]
    public void ApplicationDbContext_Registers_InitialCreateMigration()
    {
        var options = new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseSqlite("Data Source=:memory:")
            .Options;

        using var db = new ApplicationDbContext(options);
        var migrations = db.Database.GetMigrations().ToList();

        Assert.Contains("20260423114840_InitialCreate", migrations);
    }
}

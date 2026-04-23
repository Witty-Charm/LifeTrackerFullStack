using System;
using Microsoft.EntityFrameworkCore.Migrations;
using Npgsql.EntityFrameworkCore.PostgreSQL.Metadata;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "HeroAchievements",
                columns: table => new
                {
                    Id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    HeroId = table.Column<int>(type: "integer", nullable: false),
                    Key = table.Column<string>(type: "text", nullable: false),
                    UnlockedAt = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    GoldReward = table.Column<int>(type: "integer", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_HeroAchievements", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "Heroes",
                columns: table => new
                {
                    Id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    Name = table.Column<string>(type: "text", nullable: false),
                    Level = table.Column<int>(type: "integer", nullable: false),
                    CurrentXp = table.Column<long>(type: "bigint", nullable: false),
                    TotalXpEarned = table.Column<long>(type: "bigint", nullable: false),
                    CurrentHp = table.Column<int>(type: "integer", nullable: false),
                    MaxHp = table.Column<int>(type: "integer", nullable: false),
                    Gold = table.Column<int>(type: "integer", nullable: false),
                    XpBoostPercent = table.Column<int>(type: "integer", nullable: false),
                    XpBoostTasksRemaining = table.Column<int>(type: "integer", nullable: false),
                    IsShieldActive = table.Column<bool>(type: "boolean", nullable: false, defaultValue: false),
                    ShieldActivatedAtUtc = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    IsDead = table.Column<bool>(type: "boolean", nullable: false),
                    DeathTime = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    DeathCount = table.Column<int>(type: "integer", nullable: false),
                    RecoveryEndsAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    TimeZoneId = table.Column<string>(type: "text", nullable: false, defaultValue: "UTC"),
                    PendingTimeZoneId = table.Column<string>(type: "text", nullable: true),
                    TimeZoneSwitchAfterLocalDate = table.Column<string>(type: "text", nullable: true),
                    LastTimeZoneChangedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    CreatedDate = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    UpdatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    RowVersion = table.Column<byte[]>(type: "bytea", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Heroes", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "ShopItems",
                columns: table => new
                {
                    Id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    Name = table.Column<string>(type: "text", nullable: false),
                    Description = table.Column<string>(type: "text", nullable: false),
                    Price = table.Column<int>(type: "integer", nullable: false),
                    ItemType = table.Column<int>(type: "integer", nullable: false),
                    EffectValue = table.Column<int>(type: "integer", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_ShopItems", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "EconomyBalances",
                columns: table => new
                {
                    Id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    HeroId = table.Column<int>(type: "integer", nullable: false),
                    TotalGoldEarned = table.Column<long>(type: "bigint", nullable: false),
                    TotalGoldSpent = table.Column<long>(type: "bigint", nullable: false),
                    TotalXpEarned = table.Column<long>(type: "bigint", nullable: false),
                    DailyTaskCompletions = table.Column<int>(type: "integer", nullable: false),
                    MaxDailyCompletions = table.Column<int>(type: "integer", nullable: false),
                    DailyResetAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    LastDailyResetLocalDate = table.Column<string>(type: "text", nullable: true),
                    XpMultiplier = table.Column<decimal>(type: "numeric", nullable: false),
                    GoldMultiplier = table.Column<decimal>(type: "numeric", nullable: false),
                    MultiplierExpiresAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    IsInPenaltyPeriod = table.Column<bool>(type: "boolean", nullable: false),
                    PenaltyEndsAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    PenaltyMultiplier = table.Column<decimal>(type: "numeric", nullable: false),
                    CreatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    UpdatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    RowVersion = table.Column<byte[]>(type: "bytea", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_EconomyBalances", x => x.Id);
                    table.ForeignKey(
                        name: "FK_EconomyBalances_Heroes_HeroId",
                        column: x => x.HeroId,
                        principalTable: "Heroes",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "GameTasks",
                columns: table => new
                {
                    Id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    HeroId = table.Column<int>(type: "integer", nullable: false),
                    Title = table.Column<string>(type: "text", nullable: false),
                    Description = table.Column<string>(type: "text", nullable: false),
                    Type = table.Column<int>(type: "integer", nullable: false),
                    Difficulty = table.Column<int>(type: "integer", nullable: false),
                    Polarity = table.Column<int>(type: "integer", nullable: false, defaultValue: 3),
                    IsActive = table.Column<bool>(type: "boolean", nullable: false),
                    DueDate = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    RepeatPattern = table.Column<string>(type: "text", nullable: true),
                    ChecklistJson = table.Column<string>(type: "text", nullable: true),
                    RemindersJson = table.Column<string>(type: "text", nullable: true),
                    IsCompleted = table.Column<bool>(type: "boolean", nullable: false),
                    CompletionCount = table.Column<int>(type: "integer", nullable: false),
                    FailCount = table.Column<int>(type: "integer", nullable: false),
                    LastCompletedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    OverdueProcessedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    CreatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    UpdatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_GameTasks", x => x.Id);
                    table.ForeignKey(
                        name: "FK_GameTasks_Heroes_HeroId",
                        column: x => x.HeroId,
                        principalTable: "Heroes",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "Purchases",
                columns: table => new
                {
                    Id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    HeroId = table.Column<int>(type: "integer", nullable: false),
                    ShopItemId = table.Column<int>(type: "integer", nullable: false),
                    GoldSpent = table.Column<int>(type: "integer", nullable: false),
                    PurchasedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Purchases", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Purchases_Heroes_HeroId",
                        column: x => x.HeroId,
                        principalTable: "Heroes",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_Purchases_ShopItems_ShopItemId",
                        column: x => x.ShopItemId,
                        principalTable: "ShopItems",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateTable(
                name: "Streaks",
                columns: table => new
                {
                    Id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    HeroId = table.Column<int>(type: "integer", nullable: false),
                    TaskId = table.Column<int>(type: "integer", nullable: true),
                    CurrentDays = table.Column<int>(type: "integer", nullable: false),
                    LongestDays = table.Column<int>(type: "integer", nullable: false),
                    StartDate = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    LastCheckIn = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    FreezeCharges = table.Column<int>(type: "integer", nullable: false),
                    FreezeActiveUntil = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    IsShieldActive = table.Column<bool>(type: "boolean", nullable: false),
                    ShieldExpiresAtUtc = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    ShieldFailConsumed = table.Column<bool>(type: "boolean", nullable: false),
                    ShieldBackupCurrentDays = table.Column<int>(type: "integer", nullable: true),
                    ShieldBackupBreakAtUtc = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    TotalBreaks = table.Column<int>(type: "integer", nullable: false),
                    LastBreakDate = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    LastCheckInLocalDate = table.Column<string>(type: "text", nullable: true),
                    LastBreakLocalDate = table.Column<string>(type: "text", nullable: true),
                    RowVersion = table.Column<byte[]>(type: "bytea", nullable: false),
                    CreatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    UpdatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Streaks", x => x.Id);
                    table.ForeignKey(
                        name: "FK_Streaks_GameTasks_TaskId",
                        column: x => x.TaskId,
                        principalTable: "GameTasks",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_Streaks_Heroes_HeroId",
                        column: x => x.HeroId,
                        principalTable: "Heroes",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_EconomyBalances_HeroId",
                table: "EconomyBalances",
                column: "HeroId",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_GameTasks_HeroId",
                table: "GameTasks",
                column: "HeroId");

            migrationBuilder.CreateIndex(
                name: "IX_HeroAchievements_HeroId_Key",
                table: "HeroAchievements",
                columns: new[] { "HeroId", "Key" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_Purchases_HeroId",
                table: "Purchases",
                column: "HeroId");

            migrationBuilder.CreateIndex(
                name: "IX_Purchases_ShopItemId",
                table: "Purchases",
                column: "ShopItemId");

            migrationBuilder.CreateIndex(
                name: "IX_Streaks_HeroId",
                table: "Streaks",
                column: "HeroId");

            migrationBuilder.CreateIndex(
                name: "IX_Streaks_TaskId",
                table: "Streaks",
                column: "TaskId",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "EconomyBalances");

            migrationBuilder.DropTable(
                name: "HeroAchievements");

            migrationBuilder.DropTable(
                name: "Purchases");

            migrationBuilder.DropTable(
                name: "Streaks");

            migrationBuilder.DropTable(
                name: "ShopItems");

            migrationBuilder.DropTable(
                name: "GameTasks");

            migrationBuilder.DropTable(
                name: "Heroes");
        }
    }
}

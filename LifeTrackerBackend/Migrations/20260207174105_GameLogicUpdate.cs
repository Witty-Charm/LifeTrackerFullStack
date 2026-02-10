using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class GameLogicUpdate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "Heroes",
                columns: table => new
                {
                    Id = table.Column<int>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    Name = table.Column<string>(type: "TEXT", nullable: false),
                    Level = table.Column<int>(type: "INTEGER", nullable: false),
                    CurrentXp = table.Column<long>(type: "INTEGER", nullable: false),
                    TotalXpEarned = table.Column<long>(type: "INTEGER", nullable: false),
                    CurrentHp = table.Column<int>(type: "INTEGER", nullable: false),
                    MaxHp = table.Column<int>(type: "INTEGER", nullable: false),
                    Gold = table.Column<int>(type: "INTEGER", nullable: false),
                    IsDead = table.Column<bool>(type: "INTEGER", nullable: false),
                    DeathTime = table.Column<DateTime>(type: "TEXT", nullable: true),
                    DeathCount = table.Column<int>(type: "INTEGER", nullable: false),
                    RecoveryEndsAt = table.Column<DateTime>(type: "TEXT", nullable: true),
                    CreatedDate = table.Column<DateTime>(type: "TEXT", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Heroes", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "EconomyBalances",
                columns: table => new
                {
                    Id = table.Column<int>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    HeroId = table.Column<int>(type: "INTEGER", nullable: false),
                    TotalGoldEarned = table.Column<long>(type: "INTEGER", nullable: false),
                    TotalGoldSpent = table.Column<long>(type: "INTEGER", nullable: false),
                    TotalXpEarned = table.Column<long>(type: "INTEGER", nullable: false),
                    DailyTaskCompletions = table.Column<int>(type: "INTEGER", nullable: false),
                    MaxDailyCompletions = table.Column<int>(type: "INTEGER", nullable: false),
                    DailyResetAt = table.Column<DateTime>(type: "TEXT", nullable: false),
                    XpMultiplier = table.Column<decimal>(type: "TEXT", nullable: false),
                    GoldMultiplier = table.Column<decimal>(type: "TEXT", nullable: false),
                    MultiplierExpiresAt = table.Column<DateTime>(type: "TEXT", nullable: true),
                    IsInPenaltyPeriod = table.Column<bool>(type: "INTEGER", nullable: false),
                    PenaltyEndsAt = table.Column<DateTime>(type: "TEXT", nullable: true),
                    PenaltyMultiplier = table.Column<decimal>(type: "TEXT", nullable: false),
                    CreatedAt = table.Column<DateTime>(type: "TEXT", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "TEXT", nullable: false)
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
                    Id = table.Column<int>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    HeroId = table.Column<int>(type: "INTEGER", nullable: false),
                    Title = table.Column<string>(type: "TEXT", nullable: false),
                    Description = table.Column<string>(type: "TEXT", nullable: false),
                    Type = table.Column<int>(type: "INTEGER", nullable: false),
                    Difficulty = table.Column<int>(type: "INTEGER", nullable: false),
                    IsActive = table.Column<bool>(type: "INTEGER", nullable: false),
                    DueDate = table.Column<DateTime>(type: "TEXT", nullable: true),
                    RepeatPattern = table.Column<string>(type: "TEXT", nullable: true),
                    IsCompleted = table.Column<bool>(type: "INTEGER", nullable: false),
                    CompletionCount = table.Column<int>(type: "INTEGER", nullable: false),
                    FailCount = table.Column<int>(type: "INTEGER", nullable: false),
                    LastCompletedAt = table.Column<DateTime>(type: "TEXT", nullable: true),
                    CreatedAt = table.Column<DateTime>(type: "TEXT", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "TEXT", nullable: false)
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
                name: "Streaks",
                columns: table => new
                {
                    Id = table.Column<int>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    HeroId = table.Column<int>(type: "INTEGER", nullable: false),
                    TaskId = table.Column<int>(type: "INTEGER", nullable: true),
                    CurrentDays = table.Column<int>(type: "INTEGER", nullable: false),
                    LongestDays = table.Column<int>(type: "INTEGER", nullable: false),
                    StartDate = table.Column<DateTime>(type: "TEXT", nullable: true),
                    LastCheckIn = table.Column<DateTime>(type: "TEXT", nullable: true),
                    FreezeCharges = table.Column<int>(type: "INTEGER", nullable: false),
                    FreezeActiveUntil = table.Column<DateTime>(type: "TEXT", nullable: true),
                    IsShieldActive = table.Column<bool>(type: "INTEGER", nullable: false),
                    ShieldExpiresAt = table.Column<DateTime>(type: "TEXT", nullable: true),
                    TotalBreaks = table.Column<int>(type: "INTEGER", nullable: false),
                    LastBreakDate = table.Column<DateTime>(type: "TEXT", nullable: true),
                    CreatedAt = table.Column<DateTime>(type: "TEXT", nullable: false),
                    UpdatedAt = table.Column<DateTime>(type: "TEXT", nullable: false)
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
                name: "Streaks");

            migrationBuilder.DropTable(
                name: "GameTasks");

            migrationBuilder.DropTable(
                name: "Heroes");
        }
    }
}

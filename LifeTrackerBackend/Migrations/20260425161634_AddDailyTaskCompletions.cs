using System;
using Microsoft.EntityFrameworkCore.Migrations;
using Npgsql.EntityFrameworkCore.PostgreSQL.Metadata;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class AddDailyTaskCompletions : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "DailyTaskCompletions",
                columns: table => new
                {
                    Id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    HeroId = table.Column<int>(type: "integer", nullable: false),
                    TaskId = table.Column<int>(type: "integer", nullable: false),
                    LocalDate = table.Column<string>(type: "text", nullable: false),
                    IsChecked = table.Column<bool>(type: "boolean", nullable: false),
                    RewardXp = table.Column<long>(type: "bigint", nullable: false),
                    RewardGold = table.Column<int>(type: "integer", nullable: false),
                    ConsumedXpBoostCharge = table.Column<bool>(type: "boolean", nullable: false),
                    ConsumedLastXpBoostCharge = table.Column<bool>(type: "boolean", nullable: false),
                    PreviousXpBoostPercent = table.Column<int>(type: "integer", nullable: false),
                    PreviousTaskCompletionCount = table.Column<int>(type: "integer", nullable: false),
                    PreviousTaskLastCompletedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    StreakExistedBefore = table.Column<bool>(type: "boolean", nullable: false),
                    PreviousStreakCurrentDays = table.Column<int>(type: "integer", nullable: true),
                    PreviousStreakLongestDays = table.Column<int>(type: "integer", nullable: true),
                    PreviousStreakStartDate = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    PreviousStreakLastCheckIn = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: true),
                    PreviousStreakLastCheckInLocalDate = table.Column<string>(type: "text", nullable: true),
                    CreatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    UpdatedAt = table.Column<DateTimeOffset>(type: "timestamp with time zone", nullable: false),
                    RowVersion = table.Column<byte[]>(type: "bytea", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_DailyTaskCompletions", x => x.Id);
                    table.ForeignKey(
                        name: "FK_DailyTaskCompletions_GameTasks_TaskId",
                        column: x => x.TaskId,
                        principalTable: "GameTasks",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_DailyTaskCompletions_Heroes_HeroId",
                        column: x => x.HeroId,
                        principalTable: "Heroes",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_DailyTaskCompletions_HeroId_TaskId_LocalDate",
                table: "DailyTaskCompletions",
                columns: new[] { "HeroId", "TaskId", "LocalDate" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_DailyTaskCompletions_TaskId",
                table: "DailyTaskCompletions",
                column: "TaskId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "DailyTaskCompletions");
        }
    }
}

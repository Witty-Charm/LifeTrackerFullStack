using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class StreakShieldMidnight : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.RenameColumn(
                name: "ShieldExpiresAt",
                table: "Streaks",
                newName: "ShieldExpiresAtUtc");

            migrationBuilder.AddColumn<DateTimeOffset>(
                name: "ShieldBackupBreakAtUtc",
                table: "Streaks",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "ShieldBackupCurrentDays",
                table: "Streaks",
                type: "INTEGER",
                nullable: true);

            migrationBuilder.AddColumn<bool>(
                name: "ShieldFailConsumed",
                table: "Streaks",
                type: "INTEGER",
                nullable: false,
                defaultValue: false);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "ShieldBackupBreakAtUtc",
                table: "Streaks");

            migrationBuilder.DropColumn(
                name: "ShieldBackupCurrentDays",
                table: "Streaks");

            migrationBuilder.DropColumn(
                name: "ShieldFailConsumed",
                table: "Streaks");

            migrationBuilder.RenameColumn(
                name: "ShieldExpiresAtUtc",
                table: "Streaks",
                newName: "ShieldExpiresAt");
        }
    }
}

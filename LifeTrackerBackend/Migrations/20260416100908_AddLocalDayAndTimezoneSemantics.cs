using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class AddLocalDayAndTimezoneSemantics : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "LastBreakLocalDate",
                table: "Streaks",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "LastCheckInLocalDate",
                table: "Streaks",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<byte[]>(
                name: "RowVersion",
                table: "Streaks",
                type: "BLOB",
                nullable: false,
                defaultValue: new byte[0]);

            migrationBuilder.AddColumn<DateTimeOffset>(
                name: "LastTimeZoneChangedAt",
                table: "Heroes",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "PendingTimeZoneId",
                table: "Heroes",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "TimeZoneId",
                table: "Heroes",
                type: "TEXT",
                nullable: false,
                defaultValue: "UTC");

            migrationBuilder.AddColumn<string>(
                name: "TimeZoneSwitchAfterLocalDate",
                table: "Heroes",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "LastDailyResetLocalDate",
                table: "EconomyBalances",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<byte[]>(
                name: "RowVersion",
                table: "EconomyBalances",
                type: "BLOB",
                nullable: false,
                defaultValue: new byte[0]);

            migrationBuilder.Sql("""
                UPDATE Heroes
                SET TimeZoneId = 'UTC'
                WHERE TimeZoneId IS NULL OR TRIM(TimeZoneId) = '';
                """);

            migrationBuilder.Sql("""
                UPDATE Streaks
                SET LastCheckInLocalDate = CASE
                    WHEN LastCheckIn IS NOT NULL THEN strftime('%Y-%m-%d', LastCheckIn)
                    WHEN StartDate IS NOT NULL THEN strftime('%Y-%m-%d', StartDate)
                    ELSE LastCheckInLocalDate
                END
                WHERE LastCheckInLocalDate IS NULL;
                """);

            migrationBuilder.Sql("""
                UPDATE Streaks
                SET LastBreakLocalDate = strftime('%Y-%m-%d', LastBreakDate)
                WHERE LastBreakDate IS NOT NULL AND LastBreakLocalDate IS NULL;
                """);

            migrationBuilder.Sql("""
                UPDATE EconomyBalances
                SET LastDailyResetLocalDate = strftime('%Y-%m-%d', DailyResetAt)
                WHERE DailyResetAt IS NOT NULL AND LastDailyResetLocalDate IS NULL;
                """);

            migrationBuilder.Sql("""
                UPDATE Streaks
                SET RowVersion = randomblob(8)
                WHERE RowVersion = x'';
                """);

            migrationBuilder.Sql("""
                UPDATE EconomyBalances
                SET RowVersion = randomblob(8)
                WHERE RowVersion = x'';
                """);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "LastBreakLocalDate",
                table: "Streaks");

            migrationBuilder.DropColumn(
                name: "LastCheckInLocalDate",
                table: "Streaks");

            migrationBuilder.DropColumn(
                name: "RowVersion",
                table: "Streaks");

            migrationBuilder.DropColumn(
                name: "LastTimeZoneChangedAt",
                table: "Heroes");

            migrationBuilder.DropColumn(
                name: "PendingTimeZoneId",
                table: "Heroes");

            migrationBuilder.DropColumn(
                name: "TimeZoneId",
                table: "Heroes");

            migrationBuilder.DropColumn(
                name: "TimeZoneSwitchAfterLocalDate",
                table: "Heroes");

            migrationBuilder.DropColumn(
                name: "LastDailyResetLocalDate",
                table: "EconomyBalances");

            migrationBuilder.DropColumn(
                name: "RowVersion",
                table: "EconomyBalances");
        }
    }
}

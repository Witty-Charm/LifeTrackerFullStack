using System;
using LifeTracker.Data;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    [DbContext(typeof(ApplicationDbContext))]
    [Migration("20260422143000_AddHeroShieldState")]
    public partial class AddHeroShieldState : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<bool>(
                name: "IsShieldActive",
                table: "Heroes",
                type: "INTEGER",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<DateTimeOffset>(
                name: "ShieldActivatedAtUtc",
                table: "Heroes",
                type: "TEXT",
                nullable: true);

            migrationBuilder.Sql(@"
                UPDATE Heroes
                SET IsShieldActive = 1,
                    ShieldActivatedAtUtc = (
                        SELECT MIN(COALESCE(s.UpdatedAt, s.CreatedAt))
                        FROM Streaks s
                        WHERE s.HeroId = Heroes.Id
                          AND s.IsShieldActive = 1
                          AND s.ShieldExpiresAtUtc IS NOT NULL
                          AND datetime(s.ShieldExpiresAtUtc) > CURRENT_TIMESTAMP
                    )
                WHERE EXISTS (
                    SELECT 1
                    FROM Streaks s
                    WHERE s.HeroId = Heroes.Id
                      AND s.IsShieldActive = 1
                      AND s.ShieldExpiresAtUtc IS NOT NULL
                      AND datetime(s.ShieldExpiresAtUtc) > CURRENT_TIMESTAMP
                );
            ");
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "IsShieldActive",
                table: "Heroes");

            migrationBuilder.DropColumn(
                name: "ShieldActivatedAtUtc",
                table: "Heroes");
        }
    }
}

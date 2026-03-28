package com.deli.gateway.auth

import com.deli.shared.domain.model.UserRole
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class JwtTokenServiceTest :
    DescribeSpec({

        val secret = "test-secret-that-is-long-enough-for-hmac-sha256-algorithm"
        val service =
            JwtTokenService(
                secret = secret,
                expiryMinutes = 60,
                refreshExpiryDays = 30,
            )

        describe("access token") {

            it("issues a valid access token with correct claims") {
                val token =
                    service.issueAccessToken(
                        userId = "user-123",
                        email = "courier@test.com",
                        role = UserRole.COURIER,
                    )

                token shouldNotBe null
                service.extractUserId(token) shouldBe "user-123"
                service.extractEmail(token) shouldBe "courier@test.com"
                service.extractRole(token) shouldBe UserRole.COURIER
                service.isAccessToken(token) shouldBe true
                service.isRefreshToken(token) shouldBe false
            }

            it("validates a freshly issued token as Valid") {
                val token = service.issueAccessToken("u1", "a@b.com", UserRole.CUSTOMER)
                service.validateToken(token).shouldBeInstanceOf<TokenValidationResult.Valid>()
            }

            it("rejects a tampered token as Invalid") {
                val token = service.issueAccessToken("u1", "a@b.com", UserRole.CUSTOMER)
                val tampered = token.dropLast(5) + "XXXXX"
                service.validateToken(tampered).shouldBeInstanceOf<TokenValidationResult.Invalid>()
            }

            it("rejects a completely malformed string") {
                service.validateToken("not.a.jwt").shouldBeInstanceOf<TokenValidationResult.Invalid>()
            }

            it("rejects an empty string") {
                service.validateToken("").shouldBeInstanceOf<TokenValidationResult.Invalid>()
            }
        }

        describe("refresh token") {

            it("issues a refresh token identifiable as such") {
                val token = service.issueRefreshToken("user-456")
                service.isRefreshToken(token) shouldBe true
                service.isAccessToken(token) shouldBe false
                service.extractUserId(token) shouldBe "user-456"
            }
        }

        describe("expired token") {

            it("returns Expired for a token with zero-second expiry") {
                // Issue with a service configured for 0-minute expiry
                val shortLivedService =
                    JwtTokenService(
                        secret = secret,
                        expiryMinutes = 0,
                        refreshExpiryDays = 30,
                    )
                val token = shortLivedService.issueAccessToken("u1", "a@b.com", UserRole.COURIER)

                // Wait a moment for the token to expire
                Thread.sleep(1100)

                shortLivedService.validateToken(token).shouldBeInstanceOf<TokenValidationResult.Expired>()
            }
        }

        describe("token with different roles") {

            UserRole.entries.forEach { role ->
                it("correctly encodes and decodes role $role") {
                    val token = service.issueAccessToken("u1", "a@b.com", role)
                    service.extractRole(token) shouldBe role
                }
            }
        }
    })

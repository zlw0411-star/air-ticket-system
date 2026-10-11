package com.airline.ticket;

import com.airline.ticket.security.JwtService;
import com.airline.ticket.user.SysUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {
    @Test
    void generateAndParse() {
        JwtService jwtService = new JwtService("airline-ticket-system-demo-secret-key-please-change-32b", 12);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole("ADMIN");
        String token = jwtService.generate(user);
        assertEquals("1", jwtService.parse(token).getSubject());
        assertEquals("admin", jwtService.parse(token).get("username"));
    }
}

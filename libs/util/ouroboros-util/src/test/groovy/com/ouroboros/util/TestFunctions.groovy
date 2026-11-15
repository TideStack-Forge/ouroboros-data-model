package com.ouroboros.util

import static org.junit.jupiter.api.Assertions.assertEquals

import com.ouroboros.util.Functions

import org.junit.Test

class TestFunctions {

    interface IUser {
        String getUserId();
    }

    interface UserProvider {
        IUser getUser(String userId, String password);
    }

    class User implements IUser {
        @Override
        String getUserId() {
            return getId().toString()
        }

        int getId() {
            return 1
        }
    }

    @Test
    void testChain() {
        var str = Functions
                .chain { it.toString().trim() }
                .next(String::toUpperCase)
                .next { String str -> str.padLeft(10, ' ') }
                .apply("  hello  ")
        assertEquals("HELLO".padLeft(10, ' '), str)
        var number = Functions
                .chain { it.toString() }
                .next { Integer.parseInt(it) }
                .next { it * 2 }
                .next { it + 1 }
                .end("10")
        assertEquals(21, number.apply())
    }

    @Test
    void testCompose() {
        var str = Functions.compose(String::trim, String::toUpperCase)
                .apply("  hello  ")
        assertEquals("HELLO", str)

        var number = Functions.compose(Integer::parseInt, { it + 1 }, { it * 2 })
                .apply("2")
        assertEquals(6, number)
    }

    class ApiTemplateModel {
        Map<String, Object> auth;
        Map<String, String> header;
        Map<String, String> queryParams;
        Map<String, Object> body;
    }

    class BasicParams {
        String username
        String password
    }

    class AuthKeyEntity {
        String name
        String value
        String type
    }

    class AuthKeyParams {
        List<AuthKeyEntity> keys
    }

}

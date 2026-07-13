package com.acorn.abc.statistic.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

// PR #44 코드리뷰 후속: AdminAuditLogActionTypeTest와 동일한 이유로,
// 리플렉션으로 선언된 모든 상수가 isValid()를 통과하는지 확인한다.
class AdminAuditLogTargetTypeTest {

    @Test
    void 선언된_모든_상수는_isValid를_통과한다() throws IllegalAccessException {
        for (Field field : AdminAuditLogTargetType.class.getDeclaredFields()) {
            if (!isPublicStaticFinalString(field)) {
                continue;
            }
            String value = (String) field.get(null);
            assertThat(AdminAuditLogTargetType.isValid(value))
                    .as("상수 %s(%s)가 isValid()를 통과해야 한다", field.getName(), value)
                    .isTrue();
        }
    }

    private boolean isPublicStaticFinalString(Field field) {
        int modifiers = field.getModifiers();
        return field.getType() == String.class
                && Modifier.isPublic(modifiers)
                && Modifier.isStatic(modifiers)
                && Modifier.isFinal(modifiers);
    }
}

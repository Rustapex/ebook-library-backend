package com.acorn.abc.statistic.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

// PR #44 코드리뷰 후속: 상수 선언과 VALUES allowlist가 수동으로 연결돼 있어, 상수를 추가하고
// allowlist 반영을 빠뜨려도 컴파일 에러가 나지 않는다. 리플렉션으로 선언된 모든 상수를 순회해
// isValid()를 통과하는지 확인하면 이런 누락을 값을 손으로 나열하지 않고도 테스트가 잡아낸다.
class AdminAuditLogActionTypeTest {

    @Test
    void 선언된_모든_상수는_isValid를_통과한다() throws IllegalAccessException {
        for (Field field : AdminAuditLogActionType.class.getDeclaredFields()) {
            if (!isPublicStaticFinalString(field)) {
                continue;
            }
            String value = (String) field.get(null);
            assertThat(AdminAuditLogActionType.isValid(value))
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

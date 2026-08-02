package com.acorn.abc.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EpubStorageProdGuardTest {

    private static final String BUCKET = "abc-epub-private";
    private static final String REGION = "ap-northeast-2";

    @Test
    void s3와_bucket과_region이_모두있으면_통과한다() {
        assertThatCode(() -> EpubStorageProdGuard.validate("s3", BUCKET, REGION)).doesNotThrowAnyException();
    }

    @Test
    void storage가_db이면_prod기동을_실패시킨다() {
        assertThatThrownBy(() -> EpubStorageProdGuard.validate("db", BUCKET, REGION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EPUB_STORAGE");
    }

    @Test
    void storage가_비어있으면_prod기동을_실패시킨다() {
        assertThatThrownBy(() -> EpubStorageProdGuard.validate(null, BUCKET, REGION))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bucket이_비어있으면_prod기동을_실패시킨다() {
        assertThatThrownBy(() -> EpubStorageProdGuard.validate("s3", "  ", REGION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EPUB_BUCKET");
    }

    @Test
    void region이_비어있으면_prod기동을_실패시킨다() {
        assertThatThrownBy(() -> EpubStorageProdGuard.validate("s3", BUCKET, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AWS_REGION");
    }
}

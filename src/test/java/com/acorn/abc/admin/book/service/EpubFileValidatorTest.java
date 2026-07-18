package com.acorn.abc.admin.book.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class EpubFileValidatorTest {

    private final EpubFileValidator validator = new EpubFileValidator();

    @Test
    void validate_validEpub_returnsMetadata() throws Exception {
        byte[] epub = epub("application/epub+zip", false);
        MockMultipartFile file = new MockMultipartFile(
                "file", "../sample.epub", "application/epub+zip", epub);

        EpubValidationResult result = validator.validate(file);

        assertThat(result.safeFileName()).isEqualTo("sample.epub");
        assertThat(result.fileSize()).isEqualTo(epub.length);
        assertThat(result.sha256()).hasSize(64);
    }

    @Test
    void validate_invalidMimetype_rejectsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.epub", "application/epub+zip", epub("application/zip", false));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOK_EPUB_INVALID);
    }

    @Test
    void validate_encryptedEpub_rejectsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.epub", "application/epub+zip", epub("application/epub+zip", true));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOK_EPUB_INVALID);
    }

    @Test
    void validate_normalizedDuplicateEntry_rejectsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.epub", "application/epub+zip",
                epub("application/epub+zip", false, true, false, false));

        assertInvalid(file);
    }

    @Test
    void validate_manifestMissingResource_rejectsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.epub", "application/epub+zip",
                epub("application/epub+zip", false, false, true, false));

        assertInvalid(file);
    }

    @Test
    void validate_spineUnknownIdref_rejectsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.epub", "application/epub+zip",
                epub("application/epub+zip", false, false, false, true));

        assertInvalid(file);
    }

    @Test
    void validate_itemAndItemrefOutsideDirectParent_rejectsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.epub", "application/epub+zip",
                epub("application/epub+zip", false, false, false, false, true, false));

        assertInvalid(file);
    }

    @Test
    void validate_manifestItemWithoutMediaType_rejectsFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.epub", "application/epub+zip",
                epub("application/epub+zip", false, false, false, false, false, true));

        assertInvalid(file);
    }

    @Test
    void validate_fileOverLimit_usesPayloadTooLargeError() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        given(file.getSize()).willReturn(EpubFileValidator.MAX_FILE_SIZE + 1);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    ErrorCode code = ((BusinessException) error).getErrorCode();
                    assertThat(code).isEqualTo(ErrorCode.BOOK_EPUB_TOO_LARGE);
                    assertThat(code.getHttpStatus().value()).isEqualTo(413);
                });
    }

    private byte[] epub(String mimetype, boolean encrypted) throws Exception {
        return epub(mimetype, encrypted, false, false, false);
    }

    private byte[] epub(String mimetype, boolean encrypted, boolean normalizedDuplicate,
                        boolean missingResource, boolean invalidSpine) throws Exception {
        return epub(mimetype, encrypted, normalizedDuplicate, missingResource, invalidSpine, false, false);
    }

    private byte[] epub(String mimetype, boolean encrypted, boolean normalizedDuplicate,
                        boolean missingResource, boolean invalidSpine,
                        boolean invalidHierarchy, boolean missingMediaType) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/container.xml", ("""
                <?xml version="1.0" encoding="UTF-8"?>
                <container xmlns="urn:oasis:names:tc:opendocument:xmlns:container" version="1.0">
                  <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
                </container>
                """).getBytes(StandardCharsets.UTF_8));
        String spineIdref = invalidSpine ? "missing" : "chapter";
        String mediaType = missingMediaType ? "" : " media-type=\"application/xhtml+xml\"";
        String manifestItem = "<item id=\"chapter\" href=\"chapter.xhtml\"" + mediaType + "/>";
        String spineItem = "<itemref idref=\"" + spineIdref + "\"/>";
        String manifestBody = invalidHierarchy ? "<group>" + manifestItem + "</group>" : manifestItem;
        String spineBody = invalidHierarchy ? "<group>" + spineItem + "</group>" : spineItem;
        entries.put("OEBPS/content.opf", ("""
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
                  <manifest>%s</manifest>
                  <spine>%s</spine>
                </package>
                """).formatted(manifestBody, spineBody).getBytes(StandardCharsets.UTF_8));
        if (!missingResource) {
            entries.put("OEBPS/chapter.xhtml", "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body/></html>"
                    .getBytes(StandardCharsets.UTF_8));
        }
        if (normalizedDuplicate) {
            entries.put("OEBPS/./chapter.xhtml", "duplicate".getBytes(StandardCharsets.UTF_8));
        }
        if (encrypted) {
            entries.put("META-INF/encryption.xml", "<encryption/>".getBytes(StandardCharsets.UTF_8));
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            byte[] mimeBytes = mimetype.getBytes(StandardCharsets.US_ASCII);
            CRC32 crc = new CRC32();
            crc.update(mimeBytes);
            ZipEntry mimeEntry = new ZipEntry("mimetype");
            mimeEntry.setMethod(ZipEntry.STORED);
            mimeEntry.setSize(mimeBytes.length);
            mimeEntry.setCompressedSize(mimeBytes.length);
            mimeEntry.setCrc(crc.getValue());
            zip.putNextEntry(mimeEntry);
            zip.write(mimeBytes);
            zip.closeEntry();

            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private void assertInvalid(MockMultipartFile file) {
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOK_EPUB_INVALID);
    }
}

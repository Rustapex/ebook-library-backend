package com.acorn.abc.book.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;

@Component
@RequiredArgsConstructor
public class BookEpubBinaryStore {

    private final JdbcTemplate jdbcTemplate;

    public int insertNextVersion(Long bookId, String fileName, long fileSize, String sha256, MultipartFile file) {
        var lockedBooks = jdbcTemplate.query(
                "SELECT book_id FROM book WHERE book_id = ? FOR UPDATE",
                (resultSet, rowNum) -> resultSet.getLong(1), bookId);
        if (lockedBooks.isEmpty()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        Integer nextVersion = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(epub_version), 0) + 1 FROM book_epub WHERE book_id = ?",
                Integer.class, bookId);
        if (nextVersion == null) {
            throw new IllegalStateException("EPUB version calculation failed");
        }

        String sql = "INSERT INTO book_epub "
                + "(book_id, original_file_name, content_type, file_size, sha256, epub_version, epub_data, created_at) "
                + "VALUES (?, ?, 'application/epub+zip', ?, ?, ?, ?, NOW())";
        try (InputStream input = file.getInputStream()) {
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setLong(1, bookId);
                statement.setString(2, fileName);
                statement.setLong(3, fileSize);
                statement.setString(4, sha256);
                statement.setInt(5, nextVersion);
                statement.setBinaryStream(6, input, fileSize);
                return statement;
            });
            return nextVersion;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }
    }

    public void writeTo(Long bookId, int epubVersion, OutputStream output) {
        Integer rows = jdbcTemplate.query(
                "SELECT epub_data FROM book_epub WHERE book_id = ? AND epub_version = ?",
                resultSet -> {
                    if (!resultSet.next()) {
                        return 0;
                    }
                    try (InputStream input = resultSet.getBinaryStream(1)) {
                        input.transferTo(output);
                    } catch (IOException e) {
                        throw new IllegalStateException("EPUB stream write failed", e);
                    }
                    return 1;
                }, bookId, epubVersion);
        if (rows == null || rows == 0) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_NOT_FOUND);
        }
    }
}

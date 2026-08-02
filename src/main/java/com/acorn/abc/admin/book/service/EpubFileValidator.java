package com.acorn.abc.admin.book.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class EpubFileValidator {

    public static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final long MAX_UNCOMPRESSED_SIZE = 200L * 1024 * 1024;
    private static final long MAX_CONTAINER_XML_SIZE = 1024L * 1024;
    private static final long MAX_PACKAGE_XML_SIZE = 4L * 1024 * 1024;
    private static final int MAX_ENTRY_COUNT = 10_000;
    private static final String EPUB_MIME = "application/epub+zip";
    private static final String OPF_NAMESPACE = "http://www.idpf.org/2007/opf";

    public EpubValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_TOO_LARGE);
        }
        String safeName = safeFileName(file.getOriginalFilename());
        if (!safeName.toLowerCase(Locale.ROOT).endsWith(".epub")) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !EPUB_MIME.equalsIgnoreCase(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }

        try (InputStream archive = file.getInputStream();
             InputStream packageArchive = file.getInputStream();
             InputStream digestSource = file.getInputStream()) {
            ArchiveIndex index = validateArchive(archive);
            byte[] packageXml = readNamedEntry(packageArchive, index.packagePath(), MAX_PACKAGE_XML_SIZE);
            validatePackage(packageXml, index.packagePath(), index.entries());
            return new EpubValidationResult(safeName, file.getSize(), sha256(digestSource));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }
    }

    private ArchiveIndex validateArchive(InputStream input) throws Exception {
        Set<String> entries = new HashSet<>();
        byte[] containerXml = null;
        long totalSize = 0;
        int count = 0;

        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry = zip.getNextEntry();
            if (entry == null || !"mimetype".equals(entry.getName()) || entry.getMethod() != ZipEntry.STORED) {
                invalid();
            }
            byte[] mimetype = readEntry(zip, 256);
            if (!EPUB_MIME.equals(new String(mimetype, StandardCharsets.US_ASCII))) {
                invalid();
            }
            entries.add("mimetype");
            totalSize += mimetype.length;
            count++;

            while ((entry = zip.getNextEntry()) != null) {
                if (++count > MAX_ENTRY_COUNT) {
                    invalid();
                }
                String name = normalizeArchivePath(entry.getName());
                if (!entries.add(name)) {
                    invalid();
                }
                if (entry.isDirectory()) {
                    continue;
                }
                if ("META-INF/container.xml".equals(name)) {
                    containerXml = readEntry(zip, Math.min(MAX_CONTAINER_XML_SIZE,
                            MAX_UNCOMPRESSED_SIZE - totalSize));
                    totalSize += containerXml.length;
                } else {
                    totalSize += drainEntry(zip, MAX_UNCOMPRESSED_SIZE - totalSize);
                }
            }
        }

        if (containerXml == null || entries.contains("META-INF/encryption.xml")) {
            invalid();
        }
        String packagePath = normalizeArchivePath(packagePath(containerXml));
        if (!entries.contains(packagePath)) {
            invalid();
        }
        return new ArchiveIndex(Set.copyOf(entries), packagePath);
    }

    private void validatePackage(byte[] packageXml, String packagePath, Set<String> entries) throws Exception {
        Document document = parseXml(packageXml);
        Element packageElement = document.getDocumentElement();
        if (packageElement == null
                || !"package".equals(packageElement.getLocalName())
                || !OPF_NAMESPACE.equals(packageElement.getNamespaceURI())) {
            invalid();
        }

        Element manifest = requireSingleDirectChild(document, packageElement, "manifest");
        Map<String, String> manifestItems = new HashMap<>();
        for (Element item : directChildren(manifest, "item")) {
            String id = requiredAttribute(item, "id");
            String href = requiredAttribute(item, "href");
            requiredAttribute(item, "media-type");
            String referencedPath = resolvePackageReference(packagePath, href);
            if (id.isBlank() || manifestItems.putIfAbsent(id, referencedPath) != null
                    || !entries.contains(referencedPath)) {
                invalid();
            }
        }
        if (manifestItems.isEmpty()) {
            invalid();
        }

        Element spine = requireSingleDirectChild(document, packageElement, "spine");
        List<Element> itemRefs = directChildren(spine, "itemref");
        if (itemRefs.isEmpty()) {
            invalid();
        }
        for (Element itemRef : itemRefs) {
            String idref = requiredAttribute(itemRef, "idref");
            if (!manifestItems.containsKey(idref)) {
                invalid();
            }
        }
    }

    private Element requireSingleDirectChild(Document document, Element parent, String localName) {
        var candidates = document.getElementsByTagNameNS(OPF_NAMESPACE, localName);
        if (candidates.getLength() != 1 || candidates.item(0).getParentNode() != parent) {
            invalid();
        }
        return (Element) candidates.item(0);
    }

    private List<Element> directChildren(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element
                    && OPF_NAMESPACE.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) {
                result.add(element);
            }
        }
        return result;
    }

    private String requiredAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        if (value == null || value.isBlank()) {
            invalid();
        }
        return value;
    }

    private String resolvePackageReference(String packagePath, String href) throws Exception {
        if (href == null || href.isBlank()) {
            invalid();
        }
        URI uri = new URI(href.trim());
        if (uri.isAbsolute() || uri.getAuthority() != null || uri.getPath() == null || uri.getPath().isBlank()) {
            invalid();
        }
        int slash = packagePath.lastIndexOf('/');
        String packageDirectory = slash < 0 ? "" : packagePath.substring(0, slash + 1);
        return normalizeArchivePath(packageDirectory + uri.getPath());
    }

    private byte[] readNamedEntry(InputStream input, String target, long limit) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && target.equals(normalizeArchivePath(entry.getName()))) {
                    return readEntry(zip, limit);
                }
            }
        }
        invalid();
        return new byte[0];
    }

    private byte[] readEntry(ZipInputStream zip, long limit) throws Exception {
        if (limit < 0) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_TOO_LARGE);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        long total = 0;
        while ((read = zip.read(buffer)) != -1) {
            if ((total += read) > limit) {
                throw new BusinessException(ErrorCode.BOOK_EPUB_TOO_LARGE);
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private long drainEntry(ZipInputStream zip, long limit) throws Exception {
        if (limit < 0) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_TOO_LARGE);
        }
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = zip.read(buffer)) != -1) {
            if ((total += read) > limit) {
                throw new BusinessException(ErrorCode.BOOK_EPUB_TOO_LARGE);
            }
        }
        return total;
    }

    private String packagePath(byte[] xml) throws Exception {
        Document document = parseXml(xml);
        var rootfiles = document.getElementsByTagNameNS("*", "rootfile");
        if (rootfiles.getLength() != 1
                || rootfiles.item(0).getAttributes().getNamedItem("full-path") == null) {
            invalid();
        }
        return rootfiles.item(0).getAttributes().getNamedItem("full-path").getNodeValue();
    }

    private Document parseXml(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private String normalizeArchivePath(String name) {
        if (name == null || name.isBlank()) {
            invalid();
        }
        String normalized = name.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            invalid();
        }
        ArrayDeque<String> segments = new ArrayDeque<>();
        Arrays.stream(normalized.split("/"))
                .filter(segment -> !segment.isEmpty() && !".".equals(segment))
                .forEach(segment -> {
                    if ("..".equals(segment)) {
                        invalid();
                    }
                    segments.addLast(segment);
                });
        if (segments.isEmpty()) {
            invalid();
        }
        return String.join("/", segments);
    }

    private String safeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }
        String normalized = originalFilename.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (fileName.isBlank() || fileName.length() > 255) {
            throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
        }
        return fileName;
    }

    private String sha256(InputStream input) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void invalid() {
        throw new BusinessException(ErrorCode.BOOK_EPUB_INVALID);
    }

    private record ArchiveIndex(Set<String> entries, String packagePath) {
    }
}

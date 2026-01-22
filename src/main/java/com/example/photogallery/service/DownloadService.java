package com.example.photogallery.service;

import com.example.photogallery.model.Album;
import com.example.photogallery.model.Gallery;
import com.example.photogallery.model.Photo;
import com.example.photogallery.model.Tenant;
import com.example.photogallery.repository.GalleryPhotoRepository;
import com.example.photogallery.repository.GalleryRepository;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DownloadService {

    public record ResolvedDownload(
        @NonNull InputStream stream,
        @NonNull MediaType mediaType,
        @NonNull String fileName
    ) {}

    private static final Set<String> INLINE_SAFE_TYPES = Set.of(
        MediaType.IMAGE_JPEG_VALUE,
        MediaType.IMAGE_PNG_VALUE,
        MediaType.IMAGE_GIF_VALUE,
        "image/webp"
    );

    private final PhotoStorageService photoStorageService;
    private final GalleryRepository galleryRepository;
    private final GalleryPhotoRepository galleryPhotoRepository;

    private final int webMaxDimension;
    private final float webJpegQuality;

    public DownloadService(
        PhotoStorageService photoStorageService,
        GalleryRepository galleryRepository,
        GalleryPhotoRepository galleryPhotoRepository,
        @Value("${photo.gallery.download.web-max-dimension:2000}") int webMaxDimension,
        @Value("${photo.gallery.download.web-jpeg-quality:0.85}") float webJpegQuality
    ) {
        this.photoStorageService = photoStorageService;
        this.galleryRepository = galleryRepository;
        this.galleryPhotoRepository = galleryPhotoRepository;
        this.webMaxDimension = Math.max(200, webMaxDimension);
        this.webJpegQuality = Math.min(Math.max(webJpegQuality, 0.1f), 1.0f);
    }

    public ResolvedDownload openForDownload(
        Tenant tenant,
        Photo photo,
        PhotoVariant variant
    ) throws IOException {
        if (tenant == null || photo == null) {
            throw new NoSuchElementException("Photo not found");
        }
        PhotoVariant effective = variant != null ? variant : PhotoVariant.ORIGINAL;

        if (effective == PhotoVariant.WEB) {
            ResolvedDownload webVariant = openWebVariant(photo);
            if (webVariant != null) {
                return webVariant;
            }
        }

        return new ResolvedDownload(
            photoStorageService.openStream(photo.getFileName()),
            resolveMediaType(photo),
            buildOriginalDownloadName(photo)
        );
    }

    public MediaType resolveMediaType(Photo photo) {
        if (photo == null || !StringUtils.hasText(photo.getContentType())) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(photo.getContentType().trim());
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public boolean isInlineSafe(MediaType mediaType) {
        if (mediaType == null) return false;
        return INLINE_SAFE_TYPES.contains(mediaType.toString().toLowerCase(Locale.ROOT));
    }

    public void writeAlbumZip(
        OutputStream outputStream,
        Tenant tenant,
        Album album,
        PhotoVariant variant
    ) throws IOException {
        if (outputStream == null) {
            throw new IOException("Output stream required");
        }

        List<Photo> photos = listDistinctPhotosInAlbum(tenant, album);
        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            int index = 1;
            for (Photo photo : photos) {
                ResolvedDownload file;
                try {
                    file = openForDownload(tenant, photo, variant);
                } catch (java.io.FileNotFoundException e) {
                    continue;
                }

                String entryName = zipEntryName(index++, file.fileName());
                ZipEntry entry = new ZipEntry(entryName);
                zip.putNextEntry(entry);
                try (InputStream in = file.stream()) {
                    in.transferTo(zip);
                }
                zip.closeEntry();
            }
            zip.finish();
        }
    }

    public String buildAlbumZipFileName(Album album, PhotoVariant variant) {
        String base = album != null ? album.getName() : "album";
        String date = java.time.LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String suffix = variant == PhotoVariant.WEB ? "-web" : "-original";
        return sanitizeFileName(base) + "-" + date + suffix + ".zip";
    }

    private List<Photo> listDistinctPhotosInAlbum(Tenant tenant, Album album) {
        if (tenant == null || album == null) {
            return List.of();
        }

        List<Gallery> galleries = new ArrayList<>(
            galleryRepository.findByTenantAndAlbum(tenant, album)
        );
        galleries.sort(
            Comparator
                .comparing(Gallery::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Gallery::getId, Comparator.nullsLast(Comparator.naturalOrder()))
        );

        Map<Long, Photo> distinct = new LinkedHashMap<>();
        for (Gallery gallery : galleries) {
            galleryPhotoRepository
                .findByGalleryIdAndTenantOrderBySortOrderAscAddedAtAsc(
                    gallery.getId(),
                    tenant
                )
                .forEach(gp -> distinct.putIfAbsent(gp.getPhoto().getId(), gp.getPhoto()));
        }
        return distinct.values().stream().toList();
    }

    private ResolvedDownload openWebVariant(Photo photo) throws IOException {
        BufferedImage source;
        try (InputStream in = photoStorageService.openStream(photo.getFileName())) {
            source = ImageIO.read(in);
        }
        if (source == null) {
            // Non-image content (or unsupported format) – return original.
            return null;
        }

        BufferedImage scaled = scaleDown(source, webMaxDimension);
        byte[] bytes = renderJpeg(scaled, webJpegQuality);
        return new ResolvedDownload(
            new ByteArrayInputStream(bytes),
            MediaType.IMAGE_JPEG,
            buildWebDownloadName(photo)
        );
    }

    private static BufferedImage scaleDown(BufferedImage input, int maxDim) {
        int width = input.getWidth();
        int height = input.getHeight();
        if (width <= 0 || height <= 0) {
            return input;
        }

        int maxSide = Math.max(width, height);
        if (maxSide <= maxDim) {
            return input;
        }

        double scale = (double) maxDim / (double) maxSide;
        int targetW = Math.max(1, (int) Math.round(width * scale));
        int targetH = Math.max(1, (int) Math.round(height * scale));

        BufferedImage output = new BufferedImage(
            targetW,
            targetH,
            BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = output.createGraphics();
        try {
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );
            g.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY
            );
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );
            g.drawImage(input, 0, 0, targetW, targetH, null);
        } finally {
            g.dispose();
        }
        return output;
    }

    private static byte[] renderJpeg(BufferedImage image, float quality)
        throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private static String zipEntryName(int index, String fileName) {
        String safe = sanitizeFileName(StringUtils.hasText(fileName) ? fileName : "photo");
        String padded = String.format("%04d", index);
        return padded + "_" + safe;
    }

    private static String buildOriginalDownloadName(Photo photo) {
        if (photo == null) return "photo";
        if (StringUtils.hasText(photo.getOriginalName())) {
            return sanitizeFileName(photo.getOriginalName());
        }
        String ext = null;
        if (StringUtils.hasText(photo.getFileName())) {
            String leaf = photo.getFileName().trim();
            int slashIdx = leaf.lastIndexOf('/');
            if (slashIdx >= 0) {
                leaf = leaf.substring(slashIdx + 1);
            }
            int dotIdx = leaf.lastIndexOf('.');
            if (dotIdx > 0 && dotIdx < leaf.length() - 1) {
                ext = leaf.substring(dotIdx);
            }
        }
        String base = "photo-" + photo.getId();
        return ext != null ? (base + ext) : base;
    }

    private static String buildWebDownloadName(Photo photo) {
        String base = buildOriginalDownloadName(photo);
        String withoutExt = base;
        int idx = base.lastIndexOf('.');
        if (idx > 0) {
            withoutExt = base.substring(0, idx);
        }
        return sanitizeFileName(withoutExt) + "-web.jpg";
    }

    private static String sanitizeFileName(String input) {
        if (!StringUtils.hasText(input)) {
            return "file";
        }
        String trimmed = input.trim();
        String replaced = trimmed
            .replace("\\", "_")
            .replace("/", "_")
            .replace("\u0000", "");
        replaced = replaced.replaceAll("[\\r\\n\\t]+", " ").trim();
        replaced = replaced.replaceAll("[^A-Za-z0-9._ -]+", "_");
        replaced = replaced.replaceAll(" +", " ");
        if (replaced.isBlank()) {
            return "file";
        }
        return replaced.length() > 120 ? replaced.substring(0, 120) : replaced;
    }
}

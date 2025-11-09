package com.example.photogallery.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.example.photogallery.model.Photo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ExifService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void extractAndSetExifData(Photo photo, byte[] imageBytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(
                new ByteArrayInputStream(imageBytes)
            );

            extractCameraInfo(photo, metadata);
            extractDateTaken(photo, metadata);
            extractGpsData(photo, metadata);
            extractCameraSettings(photo, metadata);
            extractImageDimensions(photo, metadata);
            extractOrientation(photo, metadata);

            extractAllExifData(photo, metadata);
        } catch (ImageProcessingException | IOException e) {
            System.err.println("Error extracting EXIF data: " + e.getMessage());
        }
    }

    private void extractCameraInfo(Photo photo, Metadata metadata) {
        ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(
            ExifIFD0Directory.class
        );
        if (directory != null) {
            String make = directory.getString(ExifIFD0Directory.TAG_MAKE);
            String model = directory.getString(ExifIFD0Directory.TAG_MODEL);
            if (make != null && model != null) {
                photo.setCamera(make.trim() + " " + model.trim());
            } else if (model != null) {
                photo.setCamera(model.trim());
            }
        }
    }

    private void extractDateTaken(Photo photo, Metadata metadata) {
        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(
            ExifSubIFDDirectory.class
        );
        if (directory != null) {
            Date date = directory.getDate(
                ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL
            );
            if (date != null) {
                SimpleDateFormat formatter = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss"
                );
                photo.setDateTaken(formatter.format(date));
            }
        }
    }

    private void extractGpsData(Photo photo, Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(
            GpsDirectory.class
        );
        if (gpsDirectory != null) {
            try {
                Double latitude = gpsDirectory.getGeoLocation() != null
                    ? gpsDirectory.getGeoLocation().getLatitude()
                    : null;
                Double longitude = gpsDirectory.getGeoLocation() != null
                    ? gpsDirectory.getGeoLocation().getLongitude()
                    : null;

                if (latitude != null) {
                    photo.setGpsLatitude(String.valueOf(latitude));
                }
                if (longitude != null) {
                    photo.setGpsLongitude(String.valueOf(longitude));
                }
            } catch (Exception e) {
                // GPS data not available or parsing failed
            }
        }
    }

    private void extractCameraSettings(Photo photo, Metadata metadata) {
        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(
            ExifSubIFDDirectory.class
        );
        if (directory != null) {
            // Focal length
            if (directory.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                photo.setFocalLength(
                    directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)
                );
            }

            // Aperture (F-stop)
            if (directory.containsTag(ExifSubIFDDirectory.TAG_APERTURE)) {
                photo.setAperture(
                    directory.getString(ExifSubIFDDirectory.TAG_APERTURE)
                );
            }

            // Shutter speed
            if (directory.containsTag(ExifSubIFDDirectory.TAG_SHUTTER_SPEED)) {
                photo.setShutterSpeed(
                    directory.getString(ExifSubIFDDirectory.TAG_SHUTTER_SPEED)
                );
            }

            // ISO
            if (directory.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)) {
                photo.setIso(
                    directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)
                );
            }
        }
    }

    private void extractImageDimensions(Photo photo, Metadata metadata) {
        // Try JPEG directory first
        JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(
            JpegDirectory.class
        );
        if (jpegDirectory != null) {
            if (jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_WIDTH)) {
                Integer width = jpegDirectory.getInteger(
                    JpegDirectory.TAG_IMAGE_WIDTH
                );
                if (width != null) {
                    photo.setImageWidth(String.valueOf(width));
                }
            }
            if (jpegDirectory.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)) {
                Integer height = jpegDirectory.getInteger(
                    JpegDirectory.TAG_IMAGE_HEIGHT
                );
                if (height != null) {
                    photo.setImageHeight(String.valueOf(height));
                }
            }
        }

        // Fallback to EXIF directory
        if (photo.getImageWidth() == null || photo.getImageHeight() == null) {
            ExifSubIFDDirectory exifDirectory =
                metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDirectory != null) {
                if (
                    photo.getImageWidth() == null &&
                    exifDirectory.containsTag(
                        ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH
                    )
                ) {
                    Integer width = exifDirectory.getInteger(
                        ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH
                    );
                    if (width != null) {
                        photo.setImageWidth(String.valueOf(width));
                    }
                }
                if (
                    photo.getImageHeight() == null &&
                    exifDirectory.containsTag(
                        ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT
                    )
                ) {
                    Integer height = exifDirectory.getInteger(
                        ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT
                    );
                    if (height != null) {
                        photo.setImageHeight(String.valueOf(height));
                    }
                }
            }
        }
    }

    private void extractOrientation(Photo photo, Metadata metadata) {
        ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(
            ExifIFD0Directory.class
        );
        if (
            directory != null &&
            directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)
        ) {
            photo.setOrientation(
                directory.getString(ExifIFD0Directory.TAG_ORIENTATION)
            );
        }
    }

    private void extractAllExifData(Photo photo, Metadata metadata) {
        try {
            Map<String, Object> allData = new HashMap<>();

            for (Directory directory : metadata.getDirectories()) {
                Map<String, String> directoryData = new HashMap<>();
                for (Tag tag : directory.getTags()) {
                    directoryData.put(tag.getTagName(), tag.getDescription());
                }
                allData.put(directory.getName(), directoryData);
            }

            photo.setAllExifData(objectMapper.writeValueAsString(allData));
        } catch (Exception e) {
            System.err.println(
                "Error serializing EXIF data: " + e.getMessage()
            );
        }
    }
}

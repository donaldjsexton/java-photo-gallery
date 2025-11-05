package com.example.photogallery.service;

import com.example.photogallery.model.Photo;
import com.example.photogallery.repository.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class PhotoService {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private PhotoStorageService photoStorageService;

    public Photo savePhoto(MultipartFile file) throws IOException {
        String fileName = photoStorageService.storeFile(file);

        Photo photo = new Photo(
                file.getOriginalFilename(),
                fileName,
                file.getContentType(),
                file.getSize());


        return photoRepository.save(photo);
    }

    public List<Photo> getAllPhotos() {
        return photoRepository.findAll();

    }

    public Photo getPhotoById(Long id) {
        return photoRepository.findById(id).orElse(null);
    }
}

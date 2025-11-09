package com.example.photogallery.service;

import com.example.photogallery.model.Photo;
import com.example.photogallery.repository.PhotoRepository;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PhotoSearchService {

    @Autowired
    private PhotoRepository photoRepository;

    public List<Photo> searchPhotos(String query, Pageable pageable) {
        return photoRepository.findByTextSearch(query, pageable).getContent();
    }

    public List<String> getSearchSuggestions(String partial) {
        return List.of();
    }

    public Map<String, Long> getSearchFacets() {
        return Map.of();
    }
}

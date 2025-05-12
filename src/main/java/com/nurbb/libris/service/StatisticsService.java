package com.nurbb.libris.service;

import com.nurbb.libris.model.dto.response.LibraryStatisticsResponse;

import java.util.Map;

public interface StatisticsService {

    LibraryStatisticsResponse getLibraryStatistics();
    Map<String, Object> getOverdueBookStatistics();

}

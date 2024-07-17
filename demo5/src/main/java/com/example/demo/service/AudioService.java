package com.example.demo.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;


public interface AudioService {
    String uploadAudio(MultipartFile file);
    String transcribeAudio(MultipartFile audioFile);


}

package com.example.demo.controller;

import com.example.demo.service.impl.AsemblyAudioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/audio")
public class AssemblyController {

    @Autowired
    private AsemblyAudioService audioService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            String transcription = audioService.uploadAudio(file);
            return ResponseEntity.ok(transcription);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading and transcribing audio: " + e.getMessage());
        }
    }
}

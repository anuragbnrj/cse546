package in.anuragbanerjee.webtier.controller;

import in.anuragbanerjee.webtier.service.FileService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class HomeController {

    private final FileService fileService;

    public HomeController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/")
    public String home() {
        return "Hello from Home Controller!";
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam(value = "myfile")MultipartFile file) throws IOException {
        return ResponseEntity.ok(fileService.uploadFile(file));
    }

}

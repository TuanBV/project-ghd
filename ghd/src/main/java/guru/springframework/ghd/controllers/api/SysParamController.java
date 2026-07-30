package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.dto.sysparam.SysParamRequest;
import guru.springframework.ghd.dto.sysparam.SysParamResponse;
import guru.springframework.ghd.services.SysParamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sys-param")
public class SysParamController extends BaseController {

    private final SysParamService sysParamService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<SysParamResponse> pageParams = sysParamService.getAll();
        return ok(pageParams);
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<?> getByKey(@PathVariable String key) {
        return sysParamService.getByKey(key)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<?> updateSysParam(@Valid @RequestBody List<SysParamRequest> sysParamRequests) {
        sysParamService.updateSysParam(sysParamRequests);
        return ok(null);
    }

    @PostMapping("/sitemap")
    public ResponseEntity<?> uploadSitemap(@RequestParam("file") MultipartFile file) {
        return uploadSeoFile("sitemap", file);
    }

    @PostMapping("/seo-file/{type}")
    public ResponseEntity<?> uploadSeoFile(@PathVariable String type, @RequestParam("file") MultipartFile file) {
        try {
            sysParamService.uploadSeoFile(type, file);
            return ok("SEO file uploaded successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not process SEO file: " + e.getMessage());
        }
    }
}

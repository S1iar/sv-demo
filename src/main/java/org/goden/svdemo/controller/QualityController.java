package org.goden.svdemo.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import lombok.RequiredArgsConstructor;
import org.goden.svdemo.entity.Quality;
import org.goden.svdemo.security.service.QualityService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;

    @GetMapping("/getQuality")
    @Parameters(value = {@Parameter(name = "id", description = "质量id")})
    public Quality getQualityById(@RequestParam Long id) {
        return qualityService.getById(id);
    }

    @PostMapping("/updateQuality")
    @Parameters(value = {@Parameter(name = "quality", description = "质量信息")})
    public void updateQualityById(Quality quality) {
        qualityService.updateById(quality);
    }

    @PatchMapping("/deductQuality")
    @Parameters(value = {@Parameter(name = "id", description = "质量id"),
                        @Parameter(name = "num", description = "质量数量")})
    public void deductQualityById(@RequestBody Quality quality) {
        qualityService.deductValue(quality);
    }
}

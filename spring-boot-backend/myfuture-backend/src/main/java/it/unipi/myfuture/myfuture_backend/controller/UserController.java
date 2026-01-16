package it.unipi.myfuture.myfuture_backend.controller;

import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.service.AssetService;
import it.unipi.myfuture.myfuture_backend.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for unregistered users.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AssetService assetService;

    @Autowired
    private NewsService newsService;

    // ------------------------------------------------ start: asset API --------------------------------------------------

    @GetMapping("/assets")
    public List<AssetResponseDTO> getAllAssets() {
        return assetService.getAllAssets();
    }

    @GetMapping("/assets/{symbol}")
    public AssetResponseDTO getAssetBySymbol(@PathVariable String symbol) {
        return assetService.getAssetBySymbol(symbol);
    }

    @GetMapping("/assets/type/{type}")
    public List<AssetResponseDTO> getAssetsByType(@PathVariable AssetType type) {
        return assetService.getAssetsByType(type);
    }

    // ------------------------------------------------ end: asset API --------------------------------------------------


    // ------------------------------------------------ start: news API --------------------------------------------------

    @GetMapping("/news")
    public List<NewsResponseDTO> getAllActiveNews() {
        return newsService.getAllActiveNews();
    }

    @GetMapping("/news/sector/{sector}")
    public List<NewsResponseDTO> getNewsBySector(@PathVariable String sector) {
        return newsService.getNewsBySector(sector);
    }

    // ------------------------------------------------ end: news API --------------------------------------------------
}
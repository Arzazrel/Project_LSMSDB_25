package it.unipi.myfuture.myfuture_backend.controller;

import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.ResponseWrapper;
import it.unipi.myfuture.myfuture_backend.dto.user.UserLoginRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.UserRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.UserResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.service.AssetService;
import it.unipi.myfuture.myfuture_backend.service.NewsService;
import it.unipi.myfuture.myfuture_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for unregistered users.
 */
@RestController
@RequestMapping("/api/users")
public class UnregisteredUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private NewsService newsService;

    //-------------------------------------- start: authentication (user) API ------------------------------------------

    /**
     * register a new user
     *
     * @param request information of the new user
     * @return http response for registration
     */
    @PostMapping("/register")
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> register(
            @RequestBody UserRequestDTO request) {

        UserResponseDTO response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseWrapper<>("User registered successfully", response));
    }

    /**
     * login a registered user or admin
     *
     * @param request information to login user
     * @return http response for login
     */
    @PostMapping("/login")
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> login(
            @RequestBody UserLoginRequestDTO request) {

        UserResponseDTO response = userService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new ResponseWrapper<>("Login successful", response));
    }

    //--------------------------------------- end: authentication (user) API -------------------------------------------


    //------------------------------------------------ start: asset API --------------------------------------------------

    /**
     * get all assets
     *
     * @return http response
     */
    @GetMapping("/assets")
    public ResponseEntity<ResponseWrapper<List<AssetResponseDTO>>> getAllAssets() {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Assets retrieved successfully", assetService.getAllAssets())
        );
    }

    /**
     * get an asset by its symbol
     *
     * @param symbol asset identifier
     * @return http response
     */
    @GetMapping("/assets/{symbol}")
    public ResponseEntity<ResponseWrapper<AssetResponseDTO>> getAssetBySymbol(
            @PathVariable String symbol) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Asset retrieved successfully", assetService.getAssetBySymbol(symbol))
        );
    }

    /**
     * get all assets belonging to a type passed as a parameter
     *
     * @param type type of the asset (share, tf, crypto)
     * @return http response
     */
    @GetMapping("/assets/type/{type}")
    public ResponseEntity<ResponseWrapper<List<AssetResponseDTO>>> getAssetsByType(
            @PathVariable AssetType type) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Assets retrieved successfully", assetService.getAssetsByType(type))
        );
    }

    //------------------------------------------------ end: asset API --------------------------------------------------


    //------------------------------------------------ start: news API --------------------------------------------------

    /**
     * get all not deleted news
     *
     * @return http response
     */
    @GetMapping("/news")
    public ResponseEntity<ResponseWrapper<List<NewsResponseDTO>>> getAllActiveNews() {

        return ResponseEntity.ok(
                new ResponseWrapper<>("News retrieved successfully", newsService.getAllActiveNews())
        );
    }

    /**
     *
     * @param id news identifier
     * @return http response
     */
    @GetMapping("/news/{id}")
    public ResponseEntity<ResponseWrapper<NewsResponseDTO>> getNewsById(
            @PathVariable String id) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("News retrieved successfully", newsService.getActiveNewsById(id))
        );
    }

    /**
     * Get list of news (from offset to limit, using Redis cache if possible).
     */
    @GetMapping("/news/latest")
    public ResponseEntity<ResponseWrapper<List<NewsResponseDTO>>> getLatestActiveNews(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(new ResponseWrapper<>("Active news retrieved",
                newsService.getLimitActiveNews(offset, limit)));
    }
    //------------------------------------------------ end: news API --------------------------------------------------
}

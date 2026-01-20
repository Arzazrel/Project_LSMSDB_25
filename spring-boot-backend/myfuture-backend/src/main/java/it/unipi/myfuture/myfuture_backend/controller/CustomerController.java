package it.unipi.myfuture.myfuture_backend.controller;

import it.unipi.myfuture.myfuture_backend.dto.ResponseWrapper;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.UserRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.UserResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST Controller for authenticated customers.
 * NOTE:
 * Customer identity (userId) will be retrieved from the authentication context (JWT/JWS).
 * At the moment, userId handling is temporary and will be replaced when security is implemented.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetPriceService assetPriceService;

    @Autowired
    private NewsService newsService;

    //------------------------------------------------ start: CRUD API -------------------------------------------------
    //------------------------------------------------ start: user API -------------------------------------------------

    /**
     * Get authenticated customer information.
     */
    @GetMapping("/me")
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> getMe() {

        // TODO: retrieve userId from JWT/JWS authentication context +++++++++++++++++++++++++++++++++++++++++
        Long userId = 1L;

        return ResponseEntity.ok(
                new ResponseWrapper<>("User retrieved successfully",
                        userService.getUserById(userId))
        );
    }

    /**
     * Get cash and blocked cash of the authenticated user.
     */
    @GetMapping("/me/wallet")
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> getMeCash() {

        // TODO: retrieve userId from JWT/JWS authentication context +++++++++++++++++++++++++++++++++++++++++
        Long userId = 1L;
        // get user and get the cash and blocked cash

        return ResponseEntity.ok(
                new ResponseWrapper<>("User retrieved successfully",
                        userService.getUserById(userId))
        );
    }

    /**
     * Get wallets of the authenticated user.
     */
    @GetMapping("/me/portfolio")
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> getMePortfolio() {

        // TODO: retrieve userId from JWT/JWS authentication context +++++++++++++++++++++++++++++++++++++++++
        Long userId = 1L;
        // get user and get the wallets (share, etf, crypto)

        return ResponseEntity.ok(
                new ResponseWrapper<>("Wallet retrieved successfully",
                        userService.getUserById(userId))
        );
    }

    /**
     * Get last 10 transactions of the authenticated user.
     */
    @GetMapping("/me/lastTransactions")
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> getMeTransactions() {

        // TODO: retrieve userId from JWT/JWS authentication context +++++++++++++++++++++++++++++++++++++++++
        Long userId = 1L;
        // get last 10 transactions of the user

        return ResponseEntity.ok(
                new ResponseWrapper<>("Last 10 transactions retrieved successfully",
                        userService.getUserById(userId))
        );
    }

    /**
     * Update authenticated customer account information.
     */
    @PutMapping("/me/account")
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> updateAccount(
            @RequestBody UserRequestDTO request) {

        // TODO: retrieve userId from JWT/JWS authentication context ++++++++++++++++++++++++++++++++++++++++++
        Long userId = 1L;

        return ResponseEntity.ok(
                new ResponseWrapper<>("Account updated successfully",
                        userService.updateAccount(userId, request))
        );
    }

    //------------------------------------------------ end: user API --------------------------------------------------

    //------------------------------------------------ start: transaction API -------------------------------------------

    /**
     * Create a new transaction for the authenticated customer.
     */
    @PostMapping("/transactions")
    public ResponseEntity<ResponseWrapper<TransactionResponseDTO>> createTransaction(
            @RequestBody TransactionRequestDTO request) {

        // TODO: retrieve userId from JWT/JWS authentication context +++++++++++++++++++++++++++++++++++++++++++
        Long userId = 1L;

        return ResponseEntity.ok(
                new ResponseWrapper<>("Transaction created successfully",
                        transactionService.createTransaction(request, userId))
        );
    }

    /**
     * Get last transactions of the authenticated customer.
     */
    @GetMapping("/me/transactions")
    public ResponseEntity<ResponseWrapper<List<TransactionResponseDTO>>> getMyTransactions() {

        // TODO: retrieve userId from JWT/JWS authentication context +++++++++++++++++++++++++++++++++++++++++++++
        Long userId = 1L;

        return ResponseEntity.ok(
                new ResponseWrapper<>("Transactions retrieved successfully",
                        transactionService.getTransactionsByUser(userId))
        );
    }

    /**
     * Search transactions with optional filters. (?status=&type=&from=&to=)
     */
    @GetMapping("/transactions")
    public ResponseEntity<ResponseWrapper<List<TransactionResponseDTO>>> searchTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        // TODO: retrieve userId from JWT/JWS authentication context ++++++++++++++++++++++++++++++++++++++++++
        Long userId = 1L;

        return ResponseEntity.ok(
                new ResponseWrapper<>("Transactions retrieved successfully",
                        transactionService.searchTransactions(status, type, userId, from, to))
        );
    }

    //------------------------------------------------ end: transaction API ---------------------------------------------

    //------------------------------------------------ start: asset API --------------------------------------------------

    /**
     * Get all available assets.
     */
    @GetMapping("/assets")
    public ResponseEntity<ResponseWrapper<List<AssetResponseDTO>>> getAssets() {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Assets retrieved successfully",
                        assetService.getAllAssets())
        );
    }

    /**
     * Get asset by symbol.
     */
    @GetMapping("/assets/{symbol}")
    public ResponseEntity<ResponseWrapper<AssetResponseDTO>> getAsset(
            @PathVariable String symbol) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Asset retrieved successfully",
                        assetService.getAssetBySymbol(symbol))
        );
    }

    /**
     * Get assets by type.
     */
    @GetMapping("/assets/type/{type}")
    public ResponseEntity<ResponseWrapper<List<AssetResponseDTO>>> getAssetsByType(
            @PathVariable AssetType type) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Assets retrieved successfully",
                        assetService.getAssetsByType(type))
        );
    }

    //------------------------------------------------ end: asset API --------------------------------------------------

    //------------------------------------------------ start: asset prices API -------------------------------------------

    /**
     * Get asset prices by symbol and date range. (?from=start&to=end)
     */
    @GetMapping("/asset_prices/{symbol}")
    public ResponseEntity<ResponseWrapper<List<AssetPriceResponseDTO>>> getAssetPrices(
            @PathVariable String symbol,
            @RequestParam Instant from,
            @RequestParam Instant to) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Asset prices retrieved successfully",
                        assetPriceService.getPricesBySymbolAndDateRange(symbol, from, to))
        );
    }

    /**
     * Get latest asset price.
     */
    @GetMapping("/asset_prices/{symbol}/latest")
    public ResponseEntity<ResponseWrapper<AssetPriceResponseDTO>> getLatestPrice(
            @PathVariable String symbol) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Latest asset price retrieved successfully",
                        assetPriceService.getLatestPrice(symbol))
        );
    }

    //------------------------------------------------ end: asset prices API ---------------------------------------------

    //------------------------------------------------ start: news API --------------------------------------------------

    /**
     * Get all active news.
     */
    @GetMapping("/news")
    public ResponseEntity<ResponseWrapper<List<NewsResponseDTO>>> getNews() {

        return ResponseEntity.ok(
                new ResponseWrapper<>("News retrieved successfully",
                        newsService.getAllActiveNews())
        );
    }

    /**
     * Get news by sector.
     */
    @GetMapping("/news/sector/{sector}")
    public ResponseEntity<ResponseWrapper<List<NewsResponseDTO>>> getNewsBySector(
            @PathVariable String sector) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("News retrieved successfully",
                        newsService.getActiveNewsBySector(sector))
        );
    }

    /**
     * Get news by id.
     */
    @GetMapping("/news/{id}")
    public ResponseEntity<ResponseWrapper<NewsResponseDTO>> getNewsById(
            @PathVariable String id) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("News retrieved successfully",
                        newsService.getActiveNewsById(id))
        );
    }

    //------------------------------------------------ end: news API ---------------------------------------------------
    //------------------------------------------------- end: CRUD API --------------------------------------------------
}
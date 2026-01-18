package it.unipi.myfuture.myfuture_backend.controller;

import it.unipi.myfuture.myfuture_backend.dto.ResponseWrapper;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.UserResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.UserSuspendDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST Controller for administrators.
 * Full access to system resources.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetPriceService assetPriceService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private NewsService newsService;

    //------------------------------------------------ start: CRUD API -------------------------------------------------
    //------------------------------------------------ start: user API --------------------------------------------------

    /**
     * Get all users.
     */
    @GetMapping("/customers")
    public ResponseEntity<ResponseWrapper<List<UserResponseDTO>>> getAllUsers() {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Users retrieved successfully",
                        userService.getAllUsers())
        );
    }

    /**
     * Get user by id.
     */
    @GetMapping("/customers/{id}")
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> getUserById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("User retrieved successfully",
                        userService.getUserById(id))
        );
    }

    /**
     * Suspend a user.
     */
    @PutMapping("/customers/{id}/suspend")
    public ResponseEntity<ResponseWrapper<Void>> suspendUser(
            @PathVariable Long id,
            @RequestBody UserSuspendDTO request) {

        userService.suspendUser(id, request.getReason(), request.getSuspendedAt());

        return ResponseEntity.ok(
                new ResponseWrapper<>("User suspended successfully", null)
        );
    }

    /**
     * Unsuspend a user.
     */
    @PutMapping("/customers/{id}/unSuspend")
    public ResponseEntity<ResponseWrapper<Void>> unSuspendUser(
            @PathVariable Long id) {

        userService.unSuspendUser(id);
        return ResponseEntity.ok(
                new ResponseWrapper<>("User unsuspended successfully", null)
        );
    }

    /**
     * Soft delete a user.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ResponseWrapper<Void>> deleteUser(
            @PathVariable Long id) {

        userService.softDeleteUser(id);
        return ResponseEntity.ok(
                new ResponseWrapper<>("User deleted successfully", null)
        );
    }

    //------------------------------------------------ end: user API --------------------------------------------------

    //------------------------------------------------ start: transaction API --------------------------------------------

    /**
     * Get transaction by id (admin scope).
     */
    @GetMapping("/transactions/{id}")
    public ResponseEntity<ResponseWrapper<TransactionResponseDTO>> getTransactionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Transaction retrieved successfully",
                        transactionService.getTransactionById(id))
        );
    }

    /**
     * Get user transactions.
     */
    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<ResponseWrapper<List<TransactionResponseDTO>>> getUserTransactions(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Transactions retrieved successfully",
                        transactionService.getTransactionsByUser(userId))
        );
    }

    /**
     * Search transactions with optional filters. (?status=&type=&from=&to=&userId)
     */
    @GetMapping("/transactions")
    public ResponseEntity<ResponseWrapper<List<TransactionResponseDTO>>> searchTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long userId) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Transactions retrieved successfully",
                        transactionService.searchTransactions(status, type, userId, from, to))
        );
    }

    /**
     * Update transaction status.
     */
    @PutMapping("/transactions/{id}")
    public ResponseEntity<ResponseWrapper<Void>> updateTransaction(
            @PathVariable Long id,
            @RequestParam TransactionStatus status) {

        transactionService.updateTransactionStatus(id, status);
        return ResponseEntity.ok(
                new ResponseWrapper<>("Transaction updated successfully", null)
        );
    }

    /**
     * Delete transaction.
     */
    @DeleteMapping("/transaction/{id}")
    public ResponseEntity<ResponseWrapper<Void>> deleteTransaction(
            @PathVariable Long id) {

        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(
                new ResponseWrapper<>("Transaction deleted successfully", null)
        );
    }

    //------------------------------------------------ end: transaction API ---------------------------------------------

    //------------------------------------------------ start: asset API --------------------------------------------------

    /**
     * Create a new asset.
     */
    @PostMapping("/assets")
    public ResponseEntity<ResponseWrapper<AssetResponseDTO>> createAsset(
            @RequestBody AssetRequestDTO dto) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Asset created successfully",
                        assetService.createAsset(dto))
        );
    }

    /**
     * Get assets.
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

    /**
     * Update asset metadata.
     */
    @PutMapping("/assets/{symbol}")
    public ResponseEntity<ResponseWrapper<AssetResponseDTO>> updateAsset(
            @PathVariable String symbol,
            @RequestBody AssetRequestDTO dto) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Asset updated successfully",
                        assetService.updateAsset(symbol, dto))
        );
    }

    /**
     * Delete asset (soft delete).
     */
    @DeleteMapping("/assets/{symbol}")
    public ResponseEntity<ResponseWrapper<Void>> deleteAsset(
            @PathVariable String symbol) {

        assetService.deleteAsset(symbol);
        return ResponseEntity.ok(
                new ResponseWrapper<>("Asset deleted successfully", null)
        );
    }

    //------------------------------------------------ end: asset API --------------------------------------------------

    //------------------------------------------------ start: asset prices API -------------------------------------------

    /**
     * Insert asset price.
     */
    @PostMapping("/asset_prices")
    public ResponseEntity<ResponseWrapper<AssetPriceResponseDTO>> insertAssetPrice(
            @RequestBody AssetPriceRequestDTO request) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("Asset price inserted successfully",
                        assetPriceService.savePrice(request))
        );
    }

    /**
     * Get asset prices by symbol and date range.
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

    /**
     * Delete asset prices by symbol.
     */
    @DeleteMapping("/asset_prices/{symbol}")
    public ResponseEntity<ResponseWrapper<Void>> deleteAssetPrices(
            @PathVariable String symbol) {

        assetPriceService.deletePrices(symbol);
        return ResponseEntity.ok(
                new ResponseWrapper<>("Asset prices deleted successfully", null)
        );
    }

    //------------------------------------------------ end: asset prices API ---------------------------------------------

    //------------------------------------------------ start: news API --------------------------------------------------

    /**
     * Create news.
     */
    @PostMapping("/news")
    public ResponseEntity<ResponseWrapper<NewsResponseDTO>> createNews(
            @RequestBody NewsRequestDTO request) {

        return ResponseEntity.ok(
                new ResponseWrapper<>("News created successfully",
                        newsService.saveNews(request))
        );
    }

    /**
     * Get all news.
     */
    @GetMapping("/news")
    public ResponseEntity<ResponseWrapper<List<NewsResponseDTO>>> getAllNews() {

        return ResponseEntity.ok(
                new ResponseWrapper<>("News retrieved successfully",
                        newsService.getAllNews())
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

    /**
     * Delete news.
     */
    @DeleteMapping("/news/{id}")
    public ResponseEntity<ResponseWrapper<Void>> deleteNews(
            @PathVariable String id) {

        newsService.deleteNews(id);
        return ResponseEntity.ok(
                new ResponseWrapper<>("News deleted successfully", null)
        );
    }

    //------------------------------------------------ end: news API ---------------------------------------------------
    //------------------------------------------------ end: CRUD API ---------------------------------------------------
}
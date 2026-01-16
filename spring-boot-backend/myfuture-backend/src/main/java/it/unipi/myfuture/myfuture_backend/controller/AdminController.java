package it.unipi.myfuture.myfuture_backend.controller;

import it.unipi.myfuture.myfuture_backend.dto.request.NewsRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.request.UserSuspendDTO;
import it.unipi.myfuture.myfuture_backend.dto.response.*;
import it.unipi.myfuture.myfuture_backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Controller for administrators.
 * Full access to all system resources.
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

    // ----------------------------------------------- start: user API -------------------------------------------------

    @GetMapping("/customers")
    public List<UserResponseDTO> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/customers/{id}/suspend")
    public void suspendUser(
            @PathVariable Long id,
            @RequestBody UserSuspendDTO request
    ) {
        userService.suspendUser(id, request);
    }

    @PutMapping("/customers/{id}/unsuspend")
    public void unsuspendUser(@PathVariable Long id) {
        userService.unsuspendUser(id);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.softDeleteUser(id);
    }

    // ------------------------------------------------ end: user API --------------------------------------------------


    // --------------------------------------------------
    // ----- START: ASSETS API --------------------------
    // --------------------------------------------------

    @PostMapping("/assets")
    public AssetResponseDTO createAsset(@RequestBody AssetResponseDTO dto) {
        return assetService.createAsset(dto);
    }

    @PutMapping("/assets/{symbol}")
    public AssetResponseDTO updateAsset(
            @PathVariable String symbol,
            @RequestBody AssetResponseDTO dto
    ) {
        return assetService.updateAsset(symbol, dto);
    }

    @DeleteMapping("/assets/{symbol}")
    public void deleteAsset(@PathVariable String symbol) {
        assetService.softDeleteAsset(symbol);
    }

    // --------------------------------------------------
    // ----- END: ASSETS API ----------------------------
    // --------------------------------------------------


    // --------------------------------------------------
    // ----- START: ASSET PRICES API --------------------
    // --------------------------------------------------

    @PostMapping("/asset_prices")
    public void insertAssetPrice(@RequestBody AssetPriceResponseDTO dto) {
        assetPriceService.insertPrice(dto);
    }

    @PutMapping("/asset_prices/{symbol}")
    public void updateAssetPrice(
            @PathVariable String symbol,
            @RequestBody AssetPriceResponseDTO dto
    ) {
        assetPriceService.updatePrice(symbol, dto);
    }

    @DeleteMapping("/asset_prices/{symbol}")
    public void deleteAssetPrice(@PathVariable String symbol) {
        assetPriceService.deletePrice(symbol);
    }

    // --------------------------------------------------
    // ----- END: ASSET PRICES API ----------------------
    // --------------------------------------------------


    // --------------------------------------------------
    // ----- START: TRANSACTIONS API --------------------
    // --------------------------------------------------

    @GetMapping("/transactions/{id}")
    public TransactionResponseDTO getTransactionById(@PathVariable String id) {
        return transactionService.getTransactionByIdAdmin(id);
    }

    @GetMapping("/transactions")
    public List<TransactionResponseDTO> getTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        return transactionService.getTransactionsAdmin(status, type, userId, from, to);
    }

    @PutMapping("/transactions/{id}/status")
    public void updateTransactionStatus(
            @PathVariable String id,
            @RequestParam String status
    ) {
        transactionService.updateTransactionStatus(id, status);
    }

    @DeleteMapping("/transaction/{id}")
    public void deleteTransaction(@PathVariable String id) {
        transactionService.softDeleteTransaction(id);
    }

    // --------------------------------------------------
    // ----- END: TRANSACTIONS API ----------------------
    // --------------------------------------------------


    // --------------------------------------------------
    // ----- START: NEWS API ----------------------------
    // --------------------------------------------------

    @PostMapping("/news")
    public NewsResponseDTO createNews(@RequestBody NewsRequestDTO request) {
        return newsService.createNews(request);
    }

    @PutMapping("/news/{id}")
    public NewsResponseDTO updateNews(
            @PathVariable String id,
            @RequestBody NewsRequestDTO request
    ) {
        return newsService.updateNews(id, request);
    }

    @DeleteMapping("/news/{id}")
    public void deleteNews(@PathVariable Long id) {
        newsService.softDelete(id);
    }

    // --------------------------------------------------
    // ----- END: NEWS API ------------------------------
    // --------------------------------------------------
}
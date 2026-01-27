package it.unipi.myfuture.myfuture_backend.controller;

import it.unipi.myfuture.myfuture_backend.config.UserPrincipal;
import it.unipi.myfuture.myfuture_backend.dto.ResponseWrapper;
import it.unipi.myfuture.myfuture_backend.dto.analytics.*;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.*;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

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
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> getMe(Authentication authentication) {

        // retrieve the email address of the logged-in user from the security context
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String email = principal.getUsername();

        return ResponseEntity.ok(
                new ResponseWrapper<>("User retrieved successfully",
                        userService.getUserByEmail(email))
        );
    }

    /**
     * Get cash and blocked cash of the authenticated user.
     */
    @GetMapping("/me/cash")
    public ResponseEntity<ResponseWrapper<UserCashResponseDTO>> getMeCash(Authentication authentication) {

        String email = authentication.getName();    // retrieve the email address of the logged-in user from the security context

        return ResponseEntity.ok(
                new ResponseWrapper<>("Cash retrieved", userService.getUserCash(email))
        );
    }

    /**
     * Get wallets of the authenticated user.
     */
    @GetMapping("/me/portfolio")
    public ResponseEntity<ResponseWrapper<UserPortfolioResponseDTO>> getMePortfolio(Authentication authentication) {

        String email = authentication.getName();    // retrieve the email address of the logged-in user from the security context

        return ResponseEntity.ok(
                new ResponseWrapper<>("Portfolio retrieved", userService.getUserPortfolio(email))
        );
    }

    /**
     * Get last 10 transactions of the authenticated user.
     */
    @GetMapping("/me/lastTransactions")
    public ResponseEntity<ResponseWrapper<UserTransactionsResponseDTO>> getMeTransactions(Authentication authentication) {

        String email = authentication.getName();    // retrieve the email address of the logged-in user from the security context

        return ResponseEntity.ok(
                new ResponseWrapper<>("Portfolio retrieved", userService.getUserLastTransactions(email))
        );
    }

    /**
     * Update authenticated customer account information.
     */
    @PutMapping("/me/account")
    public ResponseEntity<ResponseWrapper<UserResponseDTO>> updateAccount(
            @RequestBody UserRequestDTO request, Authentication authentication) {

        String email = authentication.getName();    // retrieve the email address of the logged-in user from the security context

        return ResponseEntity.ok(
                new ResponseWrapper<>("Account updated successfully",
                        userService.updateAccountByEmail(email, request))
        );
    }

    //------------------------------------------------ end: user API --------------------------------------------------

    //------------------------------------------------ start: transaction API -------------------------------------------

    /**
     * Create a new transaction for the authenticated customer.
     */
    @PostMapping("/transactions")
    public ResponseEntity<ResponseWrapper<TransactionResponseDTO>> createTransaction(
            @RequestBody TransactionRequestDTO request, Authentication authentication) {

        String email = authentication.getName();            // retrieve the email address of the logged-in user from the security context
        Long userId = userService.getUserIdByEmail(email);  // get user_id

        return ResponseEntity.ok(
                new ResponseWrapper<>("Transaction created successfully",
                        transactionService.createTransaction(request, userId))
        );
    }

    /**
     * Get last transactions of the authenticated customer.
     */
    @GetMapping("/me/transactions")
    public ResponseEntity<ResponseWrapper<List<TransactionResponseDTO>>> getMyTransactions(Authentication authentication) {

        String email = authentication.getName();            // retrieve the email address of the logged-in user from the security context
        Long userId = userService.getUserIdByEmail(email);  // get user_id

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
            @RequestParam(required = false) Instant to,
            Authentication authentication) {

        String email = authentication.getName();            // retrieve the email address of the logged-in user from the security context
        Long userId = userService.getUserIdByEmail(email);  // get user_id

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
    @GetMapping("/asset-prices/{symbol}")
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
    @GetMapping("/asset-prices/{symbol}/latest")
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

    //-------------------------------------------- start: Aggregation API ----------------------------------------------
    //---------------------------------------------- start: asset API --------------------------------------------------
    /**
     * Calculate number of assets by type (share / ETF / crypto)
     */
    @GetMapping("/analytics/assets/asset-type-count")
    public ResponseEntity<ResponseWrapper<List<AssetTypeCountDTO>>> getTypeCount() {
        return ResponseEntity.ok(new ResponseWrapper<>("Asset distribution retrieved", assetService.getAssetTypeDistribution()));
    }

    /**
     * Calculate top 10 sectors by number of listed share
     */
    @GetMapping("/analytics/assets/top-asset-sector")
    public ResponseEntity<ResponseWrapper<List<SectorShareCountDTO>>> getTopSectors() {
        return ResponseEntity.ok(new ResponseWrapper<>("Top sectors retrieved", assetService.getTopSectorsByShares()));
    }
    //----------------------------------------------- end: asset API ---------------------------------------------------

    //------------------------------------------- start: asset_prices API ----------------------------------------------
    /**
     * calculate the top 10 assets with the best growth decline last day/week/month.
     */
    @GetMapping("/analytics/assets_prices/top-growth-assets")
    public ResponseEntity<ResponseWrapper<List<AssetGrowthDTO>>> getTopGrowth(@RequestParam TimeWindow window) {
        return ResponseEntity.ok(new ResponseWrapper<>("Top growth assets retrieved", assetPriceService.getGrowthAnalytics(window)));
    }

    /**
     *  calculate the top 10 assets with the best worst decline last day/week/month.
     */
    @GetMapping("/analytics/assets_prices/worst-decline-assets")
    public ResponseEntity<ResponseWrapper<List<AssetGrowthDTO>>> getWorstGrowth(@RequestParam TimeWindow window) {
        return ResponseEntity.ok(new ResponseWrapper<>("Worst decline assets retrieved", assetPriceService.getWorstAnalytics(window)));
    }

    /**
     * See the 10 assets that have consistently raisen over the past week and their average daily growth/descent rate.
     */
    @GetMapping("/analytics/assets_prices/top-stable-raisen")
    public ResponseEntity<ResponseWrapper<List<AssetStableTrendDTO>>> getStableRaisen() {
        return ResponseEntity.ok(new ResponseWrapper<>("Consistent rising assets retrieved", assetPriceService.getPositiveStableTrendAnalytics()));
    }

    /**
     * See the 10 assets that have consistently fell over the past week and their average daily growth/descent rate.
     */
    @GetMapping("/analytics/assets_prices/worst-stable-fell")
    public ResponseEntity<ResponseWrapper<List<AssetStableTrendDTO>>> getStableFell() {
        return ResponseEntity.ok(new ResponseWrapper<>("Consistent falling assets retrieved", assetPriceService.getNegativeStableTrendAnalytics()));
    }
    //-------------------------------------------- end: asset_prices API -----------------------------------------------

    //----------------------------------------------- start: news API --------------------------------------------------
    /**
     * Retrieves the count of news articles per sector for a specific time window.
     */
    @GetMapping("/analytics/news/trending-sectors")
    public ResponseEntity<ResponseWrapper<List<SectorNewsCountDTO>>> getTrendingSectors(@RequestParam TimeWindow window) {
        return ResponseEntity.ok(new ResponseWrapper<>("Top news for sector retrieved", newsService.getNewsCountBySector(window)));
    }
    //------------------------------------------------ end: news API ---------------------------------------------------
    //--------------------------------------------- end: Aggregation API -----------------------------------------------
}
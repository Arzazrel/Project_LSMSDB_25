package it.unipi.myfuture.myfuture_backend.controller;

import it.unipi.myfuture.myfuture_backend.dto.ResponseWrapper;
import it.unipi.myfuture.myfuture_backend.dto.analytics.*;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.*;
import it.unipi.myfuture.myfuture_backend.enums.*;
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
    @PostMapping("/asset-prices")
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

    /**
     * Delete asset prices by symbol.
     */
    @DeleteMapping("/asset-prices/{symbol}")
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

    /**
     * Get paginated full news history for admin review.
     */
    @GetMapping("/news/latest")
    public ResponseEntity<ResponseWrapper<List<NewsResponseDTO>>> getLimitNews(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(new ResponseWrapper<>("Full news history retrieved",
                newsService.getLimitNews(offset, limit)));
    }

    /**
     * Get paginated full news history by sector for admin review.
     */
    @GetMapping("/news/latest/sector/{sector}")
    public ResponseEntity<ResponseWrapper<List<NewsResponseDTO>>> getLimitNewsBySector(
            @PathVariable String sector,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(new ResponseWrapper<>("Full sector news history retrieved",
                newsService.getLimitNewsBySector(sector, offset, limit)));
    }

    //------------------------------------------------ end: news API ---------------------------------------------------
    //------------------------------------------------ end: CRUD API ---------------------------------------------------

    //-------------------------------------------- start: Aggregation API ----------------------------------------------
    //---------------------------------------------- start: users API --------------------------------------------------

    /**
     * View the 10 users with the largest portfolios in terms of different assets.
     */
    @GetMapping("/analytics/users/top-variety")
    public ResponseEntity<ResponseWrapper<List<UserVarietyDTO>>> getTopVariety() {
        return ResponseEntity.ok(new ResponseWrapper<>("Top 10 users by variety retrieved",
                userService.getTopUsersByPortfolioVariety()));
    }

    /**
     * View the 10 users with the largest amount of a given asset in their portfolio.
     */
    @GetMapping("/analytics/users/top-holders")
    public ResponseEntity<ResponseWrapper<List<UserTopAssetHolderDTO>>> getTopHolders(
            @RequestParam String symbol,
            @RequestParam AssetType type) {
        return ResponseEntity.ok(new ResponseWrapper<>("Top 10 holders of " + symbol + " retrieved",
                userService.getTopHoldersByAsset(symbol, type)));
    }

    /**
     * View the average, minimum and maximum number of distinct assets held by users
     */
    @GetMapping("/analytics/users/global-stats")
    public ResponseEntity<ResponseWrapper<GlobalUserStatsDTO>> getGlobalStats() {
        return ResponseEntity.ok(new ResponseWrapper<>("Global assets stats retrieved",
                userService.getGlobalPortfolioStats()));
    }
    //----------------------------------------------- end: users API ---------------------------------------------------
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

    //------------------------------------------- start: transactions API ----------------------------------------------

    /**
     * Get the most traded assets based on volume and transaction count.
     */
    @GetMapping("/analytics/transactions/most-traded")
    public ResponseEntity<ResponseWrapper<List<MostTradedAssetDTO>>> getMostTraded(@RequestParam TimeWindow window) {
        // High-level overview of market activity
        return ResponseEntity.ok(new ResponseWrapper<>("Most traded assets retrieved", transactionService.getMostTradedAssets(window)));
    }

    /**
     * Analyze transaction distribution by a specific field (e.g., 'category' or 'paymentMethod').
     */
    @GetMapping("/analytics/transactions/distribution")
    public ResponseEntity<ResponseWrapper<List<TransactionDistributionDTO>>> getDistribution(
            @RequestParam TransactionGroupField groupBy, // expected 'category' or 'paymentMethod'
            @RequestParam TimeWindow window) {
        // Monitor payment preferences or asset class popularity
        return ResponseEntity.ok(new ResponseWrapper<>("Distribution statistics for transactions retrieved", transactionService.getTransactionDistribution(groupBy, window)));
    }

    /**
     * Get the total money invested (BUY operations) globally or for a specific asset.
     */
    @GetMapping("/analytics/transactions/invested-money")
    public ResponseEntity<ResponseWrapper<TotalInvestmentDTO>> getInvestedMoney(
            @RequestParam(required = false) String symbol,
            @RequestParam TimeWindow window) {
        // Track total capital flowing into the assets
        return ResponseEntity.ok(new ResponseWrapper<>("Total amount of money invested in transactions retrieved", transactionService.getTotalMoneyInvested(symbol, window)));
    }

    /**
     * Rank users by their net financial flow (Sales - Purchases).
     */
    @GetMapping("/analytics/transactions/top-net-flow")
    public ResponseEntity<ResponseWrapper<List<UserFinancialFlowDTO>>> getTopNetFlow(@RequestParam TimeWindow window) {
        // Users who are cashing out the most (Selling > Buying)
        return ResponseEntity.ok(new ResponseWrapper<>("", transactionService.getUserFinancialFlow(window, false)));
    }

    /**
     * Rank users by their net financial flow (Sales - Purchases).
     */
    @GetMapping("/analytics/transactions/worst-net-flow")
    public ResponseEntity<ResponseWrapper<List<UserFinancialFlowDTO>>> getWorstNetFlow(@RequestParam TimeWindow window) {
        // Users who are investing the most (Buying > Selling)
        return ResponseEntity.ok(new ResponseWrapper<>("", transactionService.getUserFinancialFlow(window, true)));
    }

    //-------------------------------------------- end: transactions API -----------------------------------------------

    //----------------------------------------------- start: news API --------------------------------------------------
    /**
     * Retrieves the count of news articles per sector for a specific time window.
     */
    @GetMapping("/analytics/news/trending-sectors")
    public ResponseEntity<ResponseWrapper<List<SectorNewsCountDTO>>> getTrendingSectors(@RequestParam TimeWindow window) {
        return ResponseEntity.ok(new ResponseWrapper<>("Top news for sector retrieved", newsService.getNewsCountBySector(window)));
    }

    /**
     * Retrieves the top 5 companies most mentioned in recent news articles.
     */
    @GetMapping("/analytics/news/top-mentioned-companies")
    public ResponseEntity<ResponseWrapper<List<TopMentionedAssetDTO>>> getTopMentionedCompanies(@RequestParam TimeWindow window) {
        // Identifies companies that are currently dominating the news cycle
        return ResponseEntity.ok(new ResponseWrapper<>("Top mentioned companies retrieved", newsService.getTopMentionedCompanies(window)));
    }
    //------------------------------------------------ end: news API ---------------------------------------------------

    //--------------------------------------------- end: Aggregation API -----------------------------------------------
}
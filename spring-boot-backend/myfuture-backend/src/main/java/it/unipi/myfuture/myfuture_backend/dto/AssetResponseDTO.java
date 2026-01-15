package it.unipi.myfuture.myfuture_backend.dto;

import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import lombok.Data;

/**
 * DTO used to expose asset information to users.
 */
@Data
public class AssetResponseDTO {

    private String symbol;
    private String shortName;
    private String longName;

    private AssetType type;

    private String country;
    private String sector;
    private String industry;

    private String fundFamily;

    private String currency;
}
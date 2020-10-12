package com.jessethouin.quant.intrinio;

import com.intrinio.api.SecurityApi;
import com.intrinio.invoker.ApiClient;
import com.intrinio.invoker.Configuration;
import com.intrinio.invoker.auth.ApiKeyAuth;
import com.intrinio.models.ApiResponseSecurityStockPrices;
import org.threeten.bp.LocalDate;


public class IntradayPrices {
    public static void main(String[] args) throws Exception {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        ApiKeyAuth auth = (ApiKeyAuth) defaultClient.getAuthentication("ApiKeyAuth");
        auth.setApiKey("OmU2ZDA5YTJlNWI3NmQwODExY2JjNWZmZWY2ZGZlMzk0");
        defaultClient.setAllowRetries(true);

        SecurityApi securityApi = new SecurityApi();
        String identifier = "AAPL";
        LocalDate startDate = LocalDate.of(2018,1,1);
        LocalDate endDate = LocalDate.of(2019,1, 1);
        String frequency = "daily";
        Integer pageSize = 100;
        String nextPage;

        ApiResponseSecurityStockPrices result = securityApi.getSecurityStockPrices(identifier, startDate, endDate, frequency, pageSize, null);
        do {
            nextPage = result.getNextPage();
            System.out.println(result);
        } while (nextPage != null);
    }
}

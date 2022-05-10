package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.subscriptions.AlpacaCryptoMarketSubscription;
import com.jessethouin.quant.alpaca.subscriptions.AlpacaStockMarketSubscription;
import com.jessethouin.quant.alpaca.subscriptions.AlpacaTestTradeUpdatesSubscription;
import com.jessethouin.quant.alpaca.subscriptions.AlpacaTradeUpdatesSubscription;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.repos.PortfolioRepository;
import com.jessethouin.quant.broker.Fundamental;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.CurrencyTypes;
import com.jessethouin.quant.conf.DataFeed;
import com.jessethouin.quant.conf.Instruments;
import lombok.Getter;
import net.jacobpeterson.alpaca.model.endpoint.positions.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.Disposable;
import reactor.core.Disposables;

import javax.persistence.EntityManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.Objects.requireNonNullElse;

@Component
@Transactional
public class AlpacaLive {
    private static final Logger LOG = LogManager.getLogger(AlpacaLive.class);
    private static final Disposable.Composite SPRING_COMPOSITE_DISPOSABLE = Disposables.composite();
    AlpacaTestTradeUpdatesSubscription alpacaTestTradeUpdatesSubscription;
    @Getter
    private Portfolio portfolio;
    private final List<Fundamental> fundamentalList = new ArrayList<>();
    private final PlatformTransactionManager transactionManager;
    private final PortfolioRepository portfolioRepository;
    private TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    public AlpacaLive(PortfolioRepository portfolioRepository, PlatformTransactionManager transactionManager, EntityManager entityManager, AlpacaTestTradeUpdatesSubscription alpacaTestTradeUpdatesSubscription) {
        this.portfolioRepository = portfolioRepository;
        this.transactionManager = transactionManager;
        this.entityManager = entityManager;
        this.alpacaTestTradeUpdatesSubscription = alpacaTestTradeUpdatesSubscription;
    }

    public void doLive() {
        long start = new Date().getTime();
        CONFIG.setBacktestStart(new Date(start - Duration.ofHours(CONFIG.getRecalibrateHours()).toMillis()));
        CONFIG.setBacktestEnd(new Date(start));

        transactionTemplate = new TransactionTemplate(transactionManager);

        portfolio = requireNonNullElse(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc(), Util.createEmptyPortfolio());
        savePortfolio();
        AlpacaUtil.reconcile(portfolio);
        AlpacaUtil.showAlpacaAccountInfo();

        CONFIG.getCryptoCurrencies().forEach(s -> Util.getCurrencyFromPortfolio(s, portfolio, CurrencyTypes.CRYPTO));
        CONFIG.getSecurities().forEach(s -> Util.getSecurityFromPortfolio(s, portfolio));

        LOG.info("Portfolio Cash:");
        portfolio.getCurrencies().forEach(c -> LOG.info("\t{} : {}", c.getSymbol(), c.getQuantity()));
        portfolio.getSecurities().forEach(security -> {
            Position openPosition = AlpacaUtil.getOpenPosition(security.getSymbol());
            if (openPosition != null) {
                LOG.info("\t{} : {}" + security.getSymbol(), openPosition);
            }
        });

        Currency usd = Util.getCurrencyFromPortfolio("USD", portfolio, CurrencyTypes.FIAT);
        portfolio.getCurrencies().stream().filter(currency -> currency.getCurrencyType().equals(CurrencyTypes.CRYPTO)).forEach(currency -> fundamentalList.add(new Fundamental(Instruments.CRYPTO, usd, currency)));
        portfolio.getSecurities().forEach(security -> fundamentalList.add(new Fundamental(Instruments.STOCK, security)));

        AlpacaTradeUpdatesSubscription.builder().build().subscribe();
        AlpacaCryptoMarketSubscription.builder().fundamentals(fundamentalList).bars(CONFIG.getDataFeed().equals(DataFeed.BAR)).quotes(CONFIG.getDataFeed().equals(DataFeed.QUOTE)).trades(CONFIG.getDataFeed().equals(DataFeed.TRADE)).build().subscribe();
        AlpacaStockMarketSubscription.builder().fundamentals(fundamentalList).bars(CONFIG.getDataFeed().equals(DataFeed.BAR)).quotes(CONFIG.getDataFeed().equals(DataFeed.QUOTE)).trades(CONFIG.getDataFeed().equals(DataFeed.TRADE)).build().subscribe();

        SPRING_COMPOSITE_DISPOSABLE.add(alpacaTestTradeUpdatesSubscription.subscribe());
    }

    protected void savePortfolio() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NonNull TransactionStatus status) {
                portfolio = entityManager.merge(portfolio);
            }
        });
        // fundamental are not managed by Spring/JPA, but they have elements from the merged portfolio, which is why we have to update them manually
        fundamentalList.forEach(fundamental -> fundamental.update(portfolio));
    }
}

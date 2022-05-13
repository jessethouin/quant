package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.repos.PortfolioRepository;
import com.jessethouin.quant.binance.subscriptions.*;
import com.jessethouin.quant.broker.Fundamental;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Broker;
import com.jessethouin.quant.conf.Instruments;
import io.reactivex.disposables.CompositeDisposable;
import lombok.Getter;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.Disposable.Composite;
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
public class BinanceLive {
    private static final CompositeDisposable COMPOSITE_DISPOSABLE = new CompositeDisposable();
    private static final Composite SPRING_COMPOSITE_DISPOSABLE = Disposables.composite();
    @Getter
    private Portfolio portfolio;
    private final List<Fundamental> fundamentalList = new ArrayList<>();
    private final PlatformTransactionManager transactionManager;
    private final BinanceTestOrderSubscription binanceTestOrderSubscription;
    private final PortfolioRepository portfolioRepository;
    private TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;

    public BinanceLive(PortfolioRepository portfolioRepository, PlatformTransactionManager transactionManager, EntityManager entityManager, BinanceTestOrderSubscription binanceTestOrderSubscription) {
        this.portfolioRepository = portfolioRepository;
        this.transactionManager = transactionManager;
        this.entityManager = entityManager;
        this.binanceTestOrderSubscription = binanceTestOrderSubscription;
    }

    public void doLive() {
        long start = new Date().getTime();
        CONFIG.setBacktestStart(new Date(start - Duration.ofHours(CONFIG.getRecalibrateHours()).toMillis()));
        CONFIG.setBacktestEnd(new Date(start));

        transactionTemplate = new TransactionTemplate(transactionManager);

        portfolio = requireNonNullElse(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc(), Util.createPortfolio());
        savePortfolio();
        BinanceUtil.reconcile(portfolio);
        BinanceUtil.showWallets();
        recalibrate();
        BinanceUtil.showTradingFees(portfolio);

        if (CONFIG.getBroker() == Broker.BINANCE_TEST) {
            SPRING_COMPOSITE_DISPOSABLE.add(binanceTestOrderSubscription.subscribe());
        } else {
            COMPOSITE_DISPOSABLE.add(BinanceExecutionReportsSubscription.builder().build().subscribe());
        }

        List<CurrencyPair> currencyPairs = BinanceUtil.getAllCryptoCurrencyPairs();
        // currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceStopLossSubscription.builder().fundamental(getFundamentals(currencyPair)).build().subscribe()));
        switch (CONFIG.getDataFeed()) {
            case KLINE -> currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceKlineSubscription.builder().fundamental(getFundamentals(currencyPair)).build().subscribe()));
            case TICKER -> currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceTickerSubscription.builder().fundamental(getFundamentals(currencyPair)).build().subscribe()));
            case TRADE -> currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceTradeSubscription.builder().fundamental(getFundamentals(currencyPair)).build().subscribe()));
            case ORDER_BOOK -> currencyPairs.forEach(currencyPair -> COMPOSITE_DISPOSABLE.add(BinanceOrderBookSubscription.builder().fundamental(getFundamentals(currencyPair)).build().subscribe()));
        }
    }

    private Fundamental getFundamentals(CurrencyPair currencyPair) {
        Fundamental fundamental = new Fundamental(Instruments.CRYPTO, currencyPair, portfolio);
        fundamentalList.add(fundamental);
        return fundamental;
    }

    @Scheduled(fixedRateString = "#{${recalibrateFreq} * 60 * 1000}", initialDelayString = "#{${recalibrateFreq} * 60 * 1000}")
    protected void recalibrate() {
        if (CONFIG.isRecalibrate() && !CONFIG.isBackTest() && CONFIG.getBroker().equals(Broker.BINANCE)) {
            long start = new Date().getTime();
            CONFIG.setBacktestStart(new Date(start - Duration.ofHours(CONFIG.getRecalibrateHours()).toMillis()));
            CONFIG.setBacktestEnd(new Date(start));
            Util.recalibrate(CONFIG, true);
        }
    }

    // @Scheduled(fixedRate = 1000, initialDelay = 1000)
    protected void savePortfolio() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NonNull TransactionStatus status) {
                portfolio = entityManager.merge(portfolio);
            }
        });
        // fundamental are not managed by Spring/JPA, but they have elements from the merged portfolio, which is why we have to update them manually
        fundamentalList.forEach(fundamentals -> fundamentals.update(portfolio));
    }
}